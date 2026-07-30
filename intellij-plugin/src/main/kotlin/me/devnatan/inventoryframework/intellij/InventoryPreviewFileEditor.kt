package me.devnatan.inventoryframework.intellij

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.util.Alarm
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import javax.swing.JComponent

private const val REFRESH_DEBOUNCE_MILLIS = 300

class InventoryPreviewFileEditor(private val project: Project, private val file: VirtualFile) : UserDataHolderBase(), FileEditor {

    private val panel = InventoryPreviewPanel()
    private val propertyChangeSupport = PropertyChangeSupport(this)
    private val refreshAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)

    init {
        // The editor can be reconstructed (e.g. restoring last-open tabs on startup) while the
        // project is still indexing; retry once smart mode is reached instead of caching a
        // permanent extraction failure from that race.
        DumbService.getInstance(project).runWhenSmart(::refreshPreview)
        PsiManager.getInstance(project).addPsiTreeChangeListener(
            object : PsiTreeChangeAdapter() {
                override fun childrenChanged(event: PsiTreeChangeEvent) {
                    if (event.file?.virtualFile == file) scheduleRefresh()
                }
            },
            this,
        )
    }

    private fun scheduleRefresh() {
        refreshAlarm.cancelAllRequests()
        refreshAlarm.addRequest(::refreshPreview, REFRESH_DEBOUNCE_MILLIS)
    }

    private fun refreshPreview() {
        val model = try {
            extractPreviewModel(project, file)
        } catch (e: Exception) {
            null
        }
        panel.setModel(model)
    }

    override fun getComponent(): JComponent = panel

    override fun getPreferredFocusedComponent(): JComponent = panel

    override fun getName(): String = "Preview"

    override fun setState(state: FileEditorState) {}

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = true

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        propertyChangeSupport.addPropertyChangeListener(listener)
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        propertyChangeSupport.removePropertyChangeListener(listener)
    }

    override fun getFile(): VirtualFile = file

    override fun dispose() {}
}
