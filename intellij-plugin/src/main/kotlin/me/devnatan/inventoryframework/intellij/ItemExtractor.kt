package me.devnatan.inventoryframework.intellij

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UIfExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.UUnaryExpression
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.UastBinaryOperator
import org.jetbrains.uast.UastCallKind
import org.jetbrains.uast.UastPrefixOperator
import org.jetbrains.uast.skipParenthesizedExprDown
import org.jetbrains.uast.toUElementOfType
import org.jetbrains.uast.visitor.AbstractUastVisitor

private const val ITEM_STACK_FQN = "org.bukkit.inventory.ItemStack"
private const val MATERIAL_FQN = "org.bukkit.Material"
private const val FRAMEWORK_PACKAGE_PREFIX = "me.devnatan.inventoryframework"
private val ITEM_BINDING_METHODS = setOf("withItem", "renderWith", "onRender")
private val ROW_COLUMN_FACTORY_METHODS = setOf("row", "firstRow", "lastRow", "column", "firstColumn", "lastColumn")
// Java's `==`/`!=` map to IDENTITY_EQUALS/IDENTITY_NOT_EQUALS in UAST (not EQUALS/NOT_EQUALS,
// despite both rendering as "==" in asRenderString() - EQUALS is Kotlin's structural `==`).
// Support both so this also works if the framework is ever used from Kotlin view classes.
private val NOT_EQUALS_OPERATORS = setOf(UastBinaryOperator.NOT_EQUALS, UastBinaryOperator.IDENTITY_NOT_EQUALS)
private val EQUALITY_OPERATORS = setOf(UastBinaryOperator.EQUALS, UastBinaryOperator.IDENTITY_EQUALS) + NOT_EQUALS_OPERATORS

class ItemExtractionResult(
    val indexedSlots: Map<Int, PreviewSlot>,
    val layoutBindings: Map<Char, PreviewSlot>,
    val indexedConditionalItems: Map<Int, ConditionalItem>,
    val layoutConditionalItems: Map<Char, ConditionalItem>,
)

object ItemExtractor {

    fun extract(
        uFile: UFile,
        rows: Int,
        columns: Int,
        stateIndex: Map<PsiField, PreviewStateDeclaration>,
    ): ItemExtractionResult {
        val indexedSlots = mutableMapOf<Int, PreviewSlot>()
        val layoutBindings = mutableMapOf<Char, PreviewSlot>()
        val indexedConditionalItems = mutableMapOf<Int, ConditionalItem>()
        val layoutConditionalItems = mutableMapOf<Char, ConditionalItem>()

        fun apply(target: SlotTarget, slot: PreviewSlot?) {
            if (slot == null) return
            when (target) {
                is SlotTarget.Indices -> target.slots.forEach { indexedSlots[it] = slot }
                is SlotTarget.Layout -> layoutBindings[target.character] = slot
            }
        }

        fun applyConditional(target: SlotTarget, item: ConditionalItem) {
            when (target) {
                is SlotTarget.Indices -> target.slots.forEach { indexedConditionalItems[it] = item }
                is SlotTarget.Layout -> layoutConditionalItems[target.character] = item
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
                    val receiverCall = asCallExpression(node.receiver) ?: return false
                    val target = SlotTargetResolver.resolve(receiverCall, rows, columns) ?: return false
                    if (methodName == "withItem") {
                        val conditional = resolveConditionalItem(node.valueArguments[0], stateIndex, range)
                        if (conditional != null) {
                            applyConditional(target, conditional)
                            apply(target, conditional.default)
                        } else {
                            apply(target, resolveItem(node.valueArguments[0])?.copy(sourceRange = range))
                        }
                    } else {
                        apply(target, PreviewSlot(null, dynamic = true, sourceRange = range))
                    }
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

        return ItemExtractionResult(indexedSlots, layoutBindings, indexedConditionalItems, layoutConditionalItems)
    }

    // row/column/firstRow/lastRow/firstColumn/lastColumn also have a `(BiConsumer<Integer, T> factory)`
    // overload - e.g. `render.firstRow((pos, slot) -> slot.withItem(item))` - where withItem is called
    // on the lambda's builder parameter rather than chained directly onto the row/column call. Same
    // "fill the whole row/column" heuristic as SlotTargetResolver, just with the item found by
    // searching the lambda body instead of a receiver chain.
    private fun resolveFactoryCall(node: UCallExpression, rows: Int, columns: Int): Pair<SlotTarget, UExpression>? {
        val (target, lambda) = SlotTargetResolver.resolveFactoryLambda(node, rows, columns) ?: return null
        val itemExpr = findCallArgumentInLambda(lambda, "withItem") ?: return null
        return target to itemExpr
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

    // Detects `withItem(field.get(ctx) ? thenItem : elseItem)` / `withItem(!field.get(ctx) ? ... : ...)`
    // where `field` is a known boolean mutableState. Anything else (non-boolean state, a condition
    // that isn't a direct get() read, branches that aren't plain ItemStack constructors) isn't matched -
    // the slot just falls back to whatever resolveItem() makes of it (usually "dynamic").
    private fun resolveConditionalItem(
        rawExpr: UExpression?,
        stateIndex: Map<PsiField, PreviewStateDeclaration>,
        range: TextRange?,
    ): ConditionalItem? {
        val expr = rawExpr?.skipParenthesizedExprDown() as? UIfExpression ?: return null
        if (!expr.isTernary) return null
        val match = resolveCondition(expr.condition, stateIndex) ?: return null
        val thenSlot = resolveItem(expr.thenExpression)?.copy(sourceRange = range) ?: return null
        val elseSlot = resolveItem(expr.elseExpression)?.copy(sourceRange = range) ?: return null
        val holds = match.condition.evaluate(match.declaration.initialValue) ?: return null
        val default = if (holds) thenSlot else elseSlot
        return ConditionalItem(match.condition, thenSlot, elseSlot, default)
    }

    private class ConditionMatch(val condition: PreviewCondition, val declaration: PreviewStateDeclaration)

    // Recognizes `field.get(ctx)` / `!field.get(ctx)` for a boolean state, and
    // `field.get(ctx) == literal` / `!=` (either operand order) for an int state. Anything else -
    // arbitrary boolean expressions, comparisons against non-literals, non-tracked fields - isn't
    // matched, and the item stays whatever resolveItem() alone makes of it.
    private fun resolveCondition(
        rawCondition: UExpression,
        stateIndex: Map<PsiField, PreviewStateDeclaration>,
    ): ConditionMatch? {
        val condition = rawCondition.skipParenthesizedExprDown()

        if (condition is UUnaryExpression && condition.operator == UastPrefixOperator.LOGICAL_NOT) {
            val inner = resolveCondition(condition.operand, stateIndex) ?: return null
            return ConditionMatch(negate(inner.condition), inner.declaration)
        }

        if (condition is UBinaryExpression && condition.operator in EQUALITY_OPERATORS) {
            val left = condition.leftOperand.skipParenthesizedExprDown()
            val right = condition.rightOperand.skipParenthesizedExprDown()
            val (field, literalExpr) = fieldAndLiteral(left, right, stateIndex) ?: return null
            val declaration = stateIndex.getValue(field)
            if (declaration.kind != PreviewStateKind.INT) return null
            val literalValue = (literalExpr as? ULiteralExpression)?.value as? Int ?: return null
            val negated = condition.operator in NOT_EQUALS_OPERATORS
            return ConditionMatch(PreviewCondition.IntEquals(declaration.id, literalValue, negated), declaration)
        }

        val field = resolveGetCallField(condition, stateIndex) ?: return null
        val declaration = stateIndex.getValue(field)
        if (declaration.kind != PreviewStateKind.BOOLEAN) return null
        return ConditionMatch(PreviewCondition.BooleanState(declaration.id, negated = false), declaration)
    }

    private fun negate(condition: PreviewCondition): PreviewCondition = when (condition) {
        is PreviewCondition.BooleanState -> condition.copy(negated = !condition.negated)
        is PreviewCondition.IntEquals -> condition.copy(negated = !condition.negated)
    }

    private fun fieldAndLiteral(
        left: UExpression,
        right: UExpression,
        stateIndex: Map<PsiField, PreviewStateDeclaration>,
    ): Pair<PsiField, UExpression>? {
        resolveGetCallField(left, stateIndex)?.let { return it to right }
        resolveGetCallField(right, stateIndex)?.let { return it to left }
        return null
    }

    private fun resolveGetCallField(expr: UExpression, stateIndex: Map<PsiField, PreviewStateDeclaration>): PsiField? {
        val call = asCallExpression(expr) ?: return null
        if (call.methodName != "get") return null
        val receiver = call.receiver?.skipParenthesizedExprDown() as? UReferenceExpression ?: return null
        val field = receiver.resolve() as? PsiField ?: return null
        return field.takeIf { it in stateIndex }
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
