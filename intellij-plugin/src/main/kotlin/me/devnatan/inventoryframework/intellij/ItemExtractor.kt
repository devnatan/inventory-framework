package me.devnatan.inventoryframework.intellij

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UFile
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.UastCallKind
import org.jetbrains.uast.skipParenthesizedExprDown
import org.jetbrains.uast.toUElementOfType
import org.jetbrains.uast.visitor.AbstractUastVisitor

private const val ITEM_STACK_FQN = "org.bukkit.inventory.ItemStack"
private const val MATERIAL_FQN = "org.bukkit.Material"
private const val FRAMEWORK_PACKAGE_PREFIX = "me.devnatan.inventoryframework"
private val ITEM_BINDING_METHODS = setOf("withItem", "renderWith", "onRender")
private val ROW_COLUMN_FACTORY_METHODS = setOf("row", "firstRow", "lastRow", "column", "firstColumn", "lastColumn")

private sealed class SlotTarget {
    data class Indices(val slots: List<Int>) : SlotTarget()
    data class Layout(val character: Char) : SlotTarget()
}

class ItemExtractionResult(val indexedSlots: Map<Int, PreviewSlot>, val layoutBindings: Map<Char, PreviewSlot>)

object ItemExtractor {

    fun extract(uFile: UFile, rows: Int, columns: Int): ItemExtractionResult {
        val indexedSlots = mutableMapOf<Int, PreviewSlot>()
        val layoutBindings = mutableMapOf<Char, PreviewSlot>()

        fun apply(target: SlotTarget, slot: PreviewSlot?) {
            if (slot == null) return
            when (target) {
                is SlotTarget.Indices -> target.slots.forEach { indexedSlots[it] = slot }
                is SlotTarget.Layout -> layoutBindings[target.character] = slot
            }
        }

        uFile.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                val method = node.resolve() ?: return false
                val declaringClass = method.containingClass?.qualifiedName
                if (declaringClass == null || !declaringClass.startsWith(FRAMEWORK_PACKAGE_PREFIX)) return false

                val methodName = node.methodName
                val range = node.sourcePsi?.textRange
                if (methodName in ITEM_BINDING_METHODS && node.valueArguments.size == 1) {
                    val receiverCall = node.receiver?.skipParenthesizedExprDown() as? UCallExpression ?: return false
                    val target = resolveChainTarget(receiverCall, rows, columns) ?: return false
                    val slot = if (methodName == "withItem") {
                        resolveItem(node.valueArguments[0])?.copy(sourceRange = range)
                    } else {
                        PreviewSlot(null, dynamic = true, sourceRange = range)
                    }
                    apply(target, slot)
                    return false
                }

                if (methodName in ROW_COLUMN_FACTORY_METHODS) {
                    resolveFactoryCall(node, rows, columns)?.let { (target, itemExpr) ->
                        apply(target, resolveItem(itemExpr)?.copy(sourceRange = range))
                    }
                    return false
                }

                resolveDirectItemCall(node, rows, columns)?.let { (target, itemExpr) ->
                    apply(target, resolveItem(itemExpr)?.copy(sourceRange = range))
                }
                return false
            }
        })

        return ItemExtractionResult(indexedSlots, layoutBindings)
    }

    private fun resolveChainTarget(call: UCallExpression, rows: Int, columns: Int): SlotTarget? {
        val args = call.valueArguments
        return when (call.methodName) {
            "slot" -> (args.getOrNull(0)?.evaluate() as? Int)?.let { SlotTarget.Indices(listOf(it)) }
            "firstSlot" -> SlotTarget.Indices(listOf(0))
            "lastSlot" -> SlotTarget.Indices(listOf(rows * columns - 1))
            "layoutSlot" -> (args.getOrNull(0)?.evaluate() as? Char)?.let { SlotTarget.Layout(it) }
            // row()/column() and their first/last sugar actually return the "next available slot in
            // that row/column" - not the whole row - but the idiomatic usage (see RowColumnSample) is
            // to call them in a loop to fill the entire row/column, which we can't detect from a single
            // call site without loop analysis. Treating each call as "fill the whole row/column" is
            // wrong for genuine single-slot usage but renders the common case correctly instead of
            // showing nothing at all.
            "row" -> (args.getOrNull(0)?.evaluate() as? Int)?.let { rowIndices(it, rows, columns) }
            "firstRow" -> rowIndices(1, rows, columns)
            "lastRow" -> rowIndices(rows, rows, columns)
            "column" -> (args.getOrNull(0)?.evaluate() as? Int)?.let { columnIndices(it, rows, columns) }
            "firstColumn" -> columnIndices(1, rows, columns)
            "lastColumn" -> columnIndices(columns, rows, columns)
            else -> null
        }
    }

    private fun rowIndices(row1Indexed: Int, rows: Int, columns: Int): SlotTarget.Indices? {
        if (row1Indexed !in 1..rows) return null
        val row0 = row1Indexed - 1
        return SlotTarget.Indices((0 until columns).map { row0 * columns + it })
    }

    private fun columnIndices(column1Indexed: Int, rows: Int, columns: Int): SlotTarget.Indices? {
        if (column1Indexed !in 1..columns) return null
        val column0 = column1Indexed - 1
        return SlotTarget.Indices((0 until rows).map { it * columns + column0 })
    }

    // row/column/firstRow/lastRow/firstColumn/lastColumn also have a `(BiConsumer<Integer, T> factory)`
    // overload - e.g. `render.firstRow((pos, slot) -> slot.withItem(item))` - where withItem is called
    // on the lambda's builder parameter rather than chained directly onto the row/column call. Same
    // "fill the whole row/column" heuristic as resolveChainTarget, just with the item found by
    // searching the lambda body instead of a receiver chain.
    private fun resolveFactoryCall(node: UCallExpression, rows: Int, columns: Int): Pair<SlotTarget, UExpression>? {
        val args = node.valueArguments
        val (target, lambdaArg) = when (node.methodName) {
            "row" -> if (args.size == 2) {
                (args[0].evaluate() as? Int)?.let { rowIndices(it, rows, columns) }?.let { it to args[1] }
            } else null
            "firstRow" -> if (args.size == 1) rowIndices(1, rows, columns)?.let { it to args[0] } else null
            "lastRow" -> if (args.size == 1) rowIndices(rows, rows, columns)?.let { it to args[0] } else null
            "column" -> if (args.size == 2) {
                (args[0].evaluate() as? Int)?.let { columnIndices(it, rows, columns) }?.let { it to args[1] }
            } else null
            "firstColumn" -> if (args.size == 1) columnIndices(1, rows, columns)?.let { it to args[0] } else null
            "lastColumn" -> if (args.size == 1) columnIndices(columns, rows, columns)?.let { it to args[0] } else null
            else -> null
        } ?: return null

        val lambda = lambdaArg.skipParenthesizedExprDown() as? ULambdaExpression ?: return null
        val itemExpr = findWithItemArgumentInLambda(lambda) ?: return null
        return target to itemExpr
    }

    private fun findWithItemArgumentInLambda(lambda: ULambdaExpression): UExpression? {
        var found: UExpression? = null
        lambda.body.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                if (found == null && node.methodName == "withItem" && node.valueArguments.size == 1) {
                    found = node.valueArguments[0]
                }
                return found != null
            }
        })
        return found
    }

    private fun resolveDirectItemCall(node: UCallExpression, rows: Int, columns: Int): Pair<SlotTarget, UExpression>? {
        val args = node.valueArguments
        return when (node.methodName) {
            "slot" -> if (args.size == 2) {
                val index = args[0].evaluate() as? Int ?: return null
                SlotTarget.Indices(listOf(index)) to args[1]
            } else null
            "firstSlot" -> if (args.size == 1) SlotTarget.Indices(listOf(0)) to args[0] else null
            "lastSlot" -> if (args.size == 1) SlotTarget.Indices(listOf(rows * columns - 1)) to args[0] else null
            "layoutSlot" -> if (args.size == 2) {
                val ch = args[0].evaluate() as? Char ?: return null
                SlotTarget.Layout(ch) to args[1]
            } else null
            else -> null
        }
    }

    private fun resolveItem(rawExpr: UExpression?): PreviewSlot? {
        if (rawExpr == null) return null
        val expr = rawExpr.skipParenthesizedExprDown()
        if (isNullLiteral(expr)) return null
        val material = findItemStackConstructorCall(expr)?.let { extractMaterialName(it) }
        return PreviewSlot(material = material, dynamic = material == null)
    }

    private fun isNullLiteral(expr: UExpression): Boolean = (expr as? ULiteralExpression)?.isNull == true

    private fun findItemStackConstructorCall(rawExpr: UExpression): UCallExpression? {
        val expr = rawExpr.skipParenthesizedExprDown()
        val direct = expr as? UCallExpression
        if (direct != null) {
            if (direct.kind != UastCallKind.CONSTRUCTOR_CALL) return null
            val constructedClass = direct.classReference?.resolve() as? PsiClass ?: return null
            return if (constructedClass.qualifiedName == ITEM_STACK_FQN) direct else null
        }

        val ref = expr as? UReferenceExpression ?: return null
        val variable = ref.resolve()?.toUElementOfType<UVariable>() ?: return null
        val initializer = variable.uastInitializer ?: return null
        return findItemStackConstructorCall(initializer)
    }

    private fun extractMaterialName(constructorCall: UCallExpression): String? {
        val field = (constructorCall.valueArguments.getOrNull(0) as? UReferenceExpression)?.resolve() as? PsiField
            ?: return null
        if (field.containingClass?.qualifiedName != MATERIAL_FQN) return null
        return field.name
    }
}
