package me.devnatan.inventoryframework.intellij

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElementOfType

fun extractPreviewModel(project: Project, file: VirtualFile): PreviewModel? =
    ReadAction.compute<PreviewModel?, RuntimeException> {
        val psiFile = PsiManager.getInstance(project).findFile(file) ?: return@compute null
        val uFile = psiFile.toUElementOfType<UFile>() ?: return@compute null
        ViewExtractor.extract(uFile)
    }
