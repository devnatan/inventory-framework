package me.devnatan.inventoryframework.intellij

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiModifierListOwner
import com.intellij.uast.UastHintedVisitorAdapter
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor

private const val EXPERIMENTAL_FQN = "org.jetbrains.annotations.ApiStatus.Experimental"
private const val INTERNAL_FQN = "org.jetbrains.annotations.ApiStatus.Internal"

class ExperimentalApiUsageInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val visitor = object : AbstractUastNonRecursiveVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                reportIfUnstable(node, node.resolve(), holder)
                return true
            }

            override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression): Boolean {
                reportIfUnstable(node, node.resolve(), holder)
                return true
            }
        }
        return UastHintedVisitorAdapter.create(
            holder.file.language,
            visitor,
            arrayOf(UCallExpression::class.java, USimpleNameReferenceExpression::class.java),
        )
    }

    private fun reportIfUnstable(node: UElement, resolved: PsiElement?, holder: ProblemsHolder) {
        val owner = resolved as? PsiModifierListOwner ?: return
        val message = apiStatusMessageFor(owner) ?: return
        val sourcePsi = node.sourcePsi ?: return
        holder.registerProblem(sourcePsi, message, ProblemHighlightType.LIKE_DEPRECATED)
    }

    // Checks the resolved declaration's own annotations first, then falls back to its containing
    // class - inventory-framework marks whole classes like View/ViewBuilder @ApiStatus.Experimental
    // rather than every individual member.
    private fun apiStatusMessageFor(owner: PsiModifierListOwner): String? {
        apiStatusSuffix(owner)?.let { return "This API $it" }
        val containingClass = (owner as? PsiMember)?.containingClass ?: return null
        return apiStatusSuffix(containingClass)?.let { "This API's containing class $it" }
    }

    private fun apiStatusSuffix(owner: PsiModifierListOwner): String? = when {
        owner.hasAnnotation(EXPERIMENTAL_FQN) ->
            "is marked @ApiStatus.Experimental and may change or be removed without notice"
        owner.hasAnnotation(INTERNAL_FQN) ->
            "is marked @ApiStatus.Internal and is not intended for use outside its declaring module"
        else -> null
    }
}
