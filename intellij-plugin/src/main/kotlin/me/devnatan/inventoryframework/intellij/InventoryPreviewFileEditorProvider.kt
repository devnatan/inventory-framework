package me.devnatan.inventoryframework.intellij

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class InventoryPreviewFileEditorProvider : FileEditorProvider, DumbAware {

    override fun accept(project: Project, file: VirtualFile): Boolean = false

    override fun createEditor(project: Project, file: VirtualFile): FileEditor =
        error("InventoryPreviewFileEditorProvider.createEditor() should not be reached: accept() always returns false")

    override fun getEditorTypeId(): String = "inventoryframework-preview"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR
}
