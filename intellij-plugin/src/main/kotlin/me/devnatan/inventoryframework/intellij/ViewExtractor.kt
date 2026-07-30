package me.devnatan.inventoryframework.intellij

import com.intellij.psi.PsiField
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.evaluateString
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

        val items = ItemExtractor.extract(uFile, rows, columns)
        val slots = items.indexedSlots + layoutBoundSlots(layout, items.layoutBindings, columns)

        return PreviewModel(
            viewTypeName = viewTypeFieldName ?: DEFAULT_VIEW_TYPE_NAME,
            rows = rows,
            columns = columns,
            maxSize = maxSize,
            title = title,
            layout = layout,
            slots = slots,
        )
    }

    private fun layoutBoundSlots(
        layout: List<String>?,
        layoutBindings: Map<Char, PreviewSlot>,
        columns: Int,
    ): Map<Int, PreviewSlot> {
        if (layout == null || layoutBindings.isEmpty()) return emptyMap()
        val slots = mutableMapOf<Int, PreviewSlot>()
        layout.forEachIndexed { row, rowChars ->
            rowChars.forEachIndexed { col, character ->
                layoutBindings[character]?.let { slots[row * columns + col] = it }
            }
        }
        return slots
    }

    private fun resolveViewTypeFieldName(arg: UExpression?): String? {
        val field = (arg as? UReferenceExpression)?.resolve() as? PsiField ?: return null
        if (field.containingClass?.qualifiedName != VIEW_TYPE_FQN) return null
        return field.name
    }
}
