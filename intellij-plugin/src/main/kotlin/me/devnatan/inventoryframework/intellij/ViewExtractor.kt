package me.devnatan.inventoryframework.intellij

import com.intellij.psi.PsiField
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.skipParenthesizedExprDown
import org.jetbrains.uast.visitor.AbstractUastVisitor

private const val VIEW_TYPE_FQN = "me.devnatan.inventoryframework.ViewType"
private const val VIEWS_FQN = "me.devnatan.inventoryframework.Views"
private val CONFIG_HOST_FQNS = setOf(
    "me.devnatan.inventoryframework.ViewConfigBuilder",
    "me.devnatan.inventoryframework.ViewBuilder",
)

object ViewExtractor {

    fun extract(uFile: UFile): PreviewModel {
        var viewTypeFieldName: String? = null
        var requestedSize: Int? = null
        var title: String? = null
        var layout: List<String>? = null

        uFile.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                val method = node.resolve() ?: return false
                val declaringClass = method.containingClass?.qualifiedName ?: return false

                // Views.rows(n) is sugar for builder().type(CHEST).size(n) inside Views itself, so
                // the user's own source never contains a .type()/.size() call for us to pick up.
                if (declaringClass == VIEWS_FQN && node.methodName == "rows") {
                    (node.valueArguments.getOrNull(0)?.evaluate() as? Int)?.let { requestedSize = it }
                    if (viewTypeFieldName == null) viewTypeFieldName = DEFAULT_VIEW_TYPE_NAME
                    return false
                }

                if (declaringClass !in CONFIG_HOST_FQNS) return false

                when (node.methodName) {
                    "type" -> resolveViewTypeFieldName(node.valueArguments.getOrNull(0))?.let { viewTypeFieldName = it }
                    "size" -> (node.valueArguments.getOrNull(0)?.evaluate() as? Int)?.let { requestedSize = it }
                    "title" -> title = node.valueArguments.getOrNull(0)?.evaluateString()
                    "layout" -> {
                        val chars = node.valueArguments.mapNotNull { it.evaluateString() }
                        if (chars.isNotEmpty()) layout = chars
                    }
                }
                return false
            }
        })

        val geometry = viewTypeFieldName?.let { viewGeometryFor(it) } ?: DEFAULT_VIEW_GEOMETRY
        val rows = requestedSize?.takeIf { it in 1..geometry.rows } ?: geometry.rows
        val columns = geometry.columns
        val maxSize = minOf(geometry.maxSize, rows * columns)

        val states = StateExtractor.extract(uFile)
        val items = ItemExtractor.extract(uFile, rows, columns, states.index)
        val slots = items.indexedSlots + layoutBoundValues(layout, items.layoutBindings, columns)
        val conditionalItems =
            items.indexedConditionalItems + layoutBoundValues(layout, items.layoutConditionalItems, columns)
        val clickActionResult = ClickHandlerExtractor.extract(uFile, rows, columns, states.index)
        val clickActions =
            clickActionResult.indexed + layoutBoundValues(layout, clickActionResult.layoutBound, columns)

        // availableSlot(...)'s real slot(s) depend on every call site in the file (and, inside a
        // countable loop, how many times it runs) plus everything already occupied by explicit
        // bindings, so it can only be resolved once `slots` (the explicit ones) is known - see
        // AvailableSlotResolver.
        val availableAnchors = AvailableSlotResolver.collectAnchors(uFile)
        val anchorToSlots = AvailableSlotResolver.resolve(availableAnchors, slots.keys, layout, columns, maxSize)

        return PreviewModel(
            viewTypeName = viewTypeFieldName ?: DEFAULT_VIEW_TYPE_NAME,
            rows = rows,
            columns = columns,
            maxSize = maxSize,
            title = title,
            layout = layout,
            slots = slots + remapByAnchorSequence(items.availableSlotBindings, anchorToSlots),
            states = states.declarations,
            conditionalItems = conditionalItems + remapByAnchor(items.availableSlotConditionalItems, anchorToSlots),
            clickActions = clickActions + remapByAnchor(clickActionResult.availableSlotBound, anchorToSlots),
        )
    }

    // Shared by items, conditional items and click actions - all three are keyed by layout
    // character and need to be flattened onto the same row/column grid the same way.
    private fun <T> layoutBoundValues(layout: List<String>?, bindings: Map<Char, T>, columns: Int): Map<Int, T> {
        if (layout == null || bindings.isEmpty()) return emptyMap()
        val values = mutableMapOf<Int, T>()
        layout.forEachIndexed { row, rowChars ->
            rowChars.forEachIndexed { col, character ->
                bindings[character]?.let { values[row * columns + col] = it }
            }
        }
        return values
    }

    // Shared by items, conditional items and click actions bound through availableSlot(...) - all
    // three are keyed by call-site anchor until AvailableSlotResolver assigns real slots, one call
    // site's single statically-extracted binding fanning out to every slot it claimed (more than
    // one when the call is inside a countable loop). A call site missing from anchorToSlots ran
    // out of container capacity and is dropped rather than shown at a wrong or arbitrary position.
    private fun <T> remapByAnchor(bindings: Map<Int, T>, anchorToSlots: Map<Int, List<Int>>): Map<Int, T> {
        if (bindings.isEmpty()) return emptyMap()
        val values = mutableMapOf<Int, T>()
        bindings.forEach { (anchor, value) -> anchorToSlots[anchor]?.forEach { values[it] = value } }
        return values
    }

    // Like remapByAnchor, but for item bindings that can vary per iteration - a loop's induction
    // variable read as an item's amount (see ItemExtractor.resolveAvailableSlotItems). Each
    // resolved slot gets the binding at its own position in the call site's list; if there are more
    // slots than bindings, the last binding repeats, which is exactly what a single-element list
    // (every non-varying availableSlot(...) shape) does across all of its resolved slots.
    private fun <T> remapByAnchorSequence(bindings: Map<Int, List<T>>, anchorToSlots: Map<Int, List<Int>>): Map<Int, T> {
        if (bindings.isEmpty()) return emptyMap()
        val values = mutableMapOf<Int, T>()
        bindings.forEach { (anchor, perIteration) ->
            if (perIteration.isEmpty()) return@forEach
            anchorToSlots[anchor]?.forEachIndexed { i, slot -> values[slot] = perIteration.getOrElse(i) { perIteration.last() } }
        }
        return values
    }

    private fun resolveViewTypeFieldName(arg: UExpression?): String? {
        val field = (arg?.skipParenthesizedExprDown() as? UReferenceExpression)?.resolve() as? PsiField
            ?: return null
        if (field.containingClass?.qualifiedName != VIEW_TYPE_FQN) return null
        return field.name
    }
}
