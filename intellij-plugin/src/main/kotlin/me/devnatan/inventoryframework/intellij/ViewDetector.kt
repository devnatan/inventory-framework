package me.devnatan.inventoryframework.intellij

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElementOfType
import org.jetbrains.uast.visitor.AbstractUastVisitor

private const val VIEW_FQN = "me.devnatan.inventoryframework.View"
private const val VIEWS_FQN = "me.devnatan.inventoryframework.Views"
private val VIEWS_ENTRY_POINT_METHODS = setOf("rows", "type", "builder")

object ViewDetector {

    fun isViewFile(project: Project, file: VirtualFile): Boolean {
        // InheritanceUtil/method resolution below hit the stub index, which throws
        // IndexNotReadyException while the project is still indexing (dumb mode).
        if (DumbService.isDumb(project)) return false

        return try {
            val psiFile = PsiManager.getInstance(project).findFile(file) ?: return false
            val uFile = psiFile.toUElementOfType<UFile>() ?: return false
            uFile.classes.any { InheritanceUtil.isInheritor(it, VIEW_FQN) } || containsInlineViewsEntryPoint(uFile)
        } catch (e: IndexNotReadyException) {
            false
        }
    }

    private fun containsInlineViewsEntryPoint(uFile: UFile): Boolean {
        var found = false
        uFile.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                if (!found && node.methodName in VIEWS_ENTRY_POINT_METHODS) {
                    val containingClass = node.resolve()?.containingClass?.qualifiedName
                    if (containingClass == VIEWS_FQN) {
                        found = true
                    }
                }
                return found
            }
        })
        return found
    }
}
