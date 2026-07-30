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

// Internal gets the ordinary yellow warning underline (you really shouldn't be calling this at
// all); Experimental gets the lighter gray "weak warning" dotted underline (it works, just may
// change), the same distinction IntelliJ itself uses for weaker suggestions vs real warnings.
private enum class ApiStability(val highlightType: ProblemHighlightType, val suffix: String) {
    EXPERIMENTAL(
        ProblemHighlightType.WEAK_WARNING,
        "is marked @ApiStatus.Experimental and may change or be removed without notice",
    ),
    INTERNAL(
        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
        "is marked @ApiStatus.Internal and is not intended for use outside its declaring module",
    ),
}

private data class ApiStatusFinding(val stability: ApiStability, val message: String)

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
        val finding = findApiStatus(owner) ?: return
        val sourcePsi = node.sourcePsi ?: return
        holder.registerProblem(sourcePsi, finding.message, finding.stability.highlightType)
    }

    // Checks the resolved declaration's own annotations first, then falls back to its containing
    // class - inventory-framework marks whole classes like View/ViewBuilder @ApiStatus.Experimental
    // rather than every individual member.
    private fun findApiStatus(owner: PsiModifierListOwner): ApiStatusFinding? {
        apiStabilityOf(owner)?.let { return ApiStatusFinding(it, "This API ${it.suffix}") }
        val containingClass = (owner as? PsiMember)?.containingClass ?: return null
        return apiStabilityOf(containingClass)?.let { ApiStatusFinding(it, "This API's containing class ${it.suffix}") }
    }

    private fun apiStabilityOf(owner: PsiModifierListOwner): ApiStability? = when {
        owner.hasAnnotation(EXPERIMENTAL_FQN) -> ApiStability.EXPERIMENTAL
        owner.hasAnnotation(INTERNAL_FQN) -> ApiStability.INTERNAL
        else -> null
    }
}
