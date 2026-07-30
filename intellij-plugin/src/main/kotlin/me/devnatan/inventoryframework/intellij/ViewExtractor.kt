package me.devnatan.inventoryframework.intellij

import com.intellij.psi.PsiField
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.visitor.AbstractUastVisitor

private const val VIEW_TYPE_FQN = "me.devnatan.inventoryframework.ViewType"
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
                if (method.containingClass?.qualifiedName !in CONFIG_HOST_FQNS) return false

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

        return PreviewModel(
            viewTypeName = viewTypeFieldName ?: DEFAULT_VIEW_TYPE_NAME,
            rows = rows,
            columns = geometry.columns,
            title = title,
            layout = layout,
        )
    }

    private fun resolveViewTypeFieldName(arg: UExpression?): String? {
        val field = (arg as? UReferenceExpression)?.resolve() as? PsiField ?: return null
        if (field.containingClass?.qualifiedName != VIEW_TYPE_FQN) return null
        return field.name
    }
}
