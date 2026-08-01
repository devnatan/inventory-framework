package me.devnatan.inventoryframework.intellij

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.Alarm
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import javax.swing.JComponent
import javax.swing.JPanel

private const val REFRESH_DEBOUNCE_MILLIS = 300
private const val TOOLBAR_PLACE = "InventoryFramework.PreviewToolbar"

class InventoryPreviewFileEditor(
    private val project: Project,
    private val file: VirtualFile,
    private val textEditor: TextEditor,
) : UserDataHolderBase(), FileEditor {

    private val panel = InventoryPreviewPanel()
    private val propertyChangeSupport = PropertyChangeSupport(this)
    private val refreshAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)
    private var currentModel: PreviewModel? = null
    private val rootComponent: JComponent by lazy { buildComponent() }

    init {
        panel.onSlotClicked = ::navigateToRange
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
        textEditor.editor.caretModel.addCaretListener(
            object : CaretListener {
                override fun caretPositionChanged(event: CaretEvent) = updateHighlightForCaret()
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
        currentModel = model
        panel.setModel(model)
        updateHighlightForCaret()
    }

    private fun updateHighlightForCaret() {
        val offset = textEditor.editor.caretModel.offset
        val slotIndices = currentModel?.slots?.entries
            ?.filter { (_, slot) -> slot.sourceRange?.containsOffset(offset) == true }
            ?.map { it.key }
            ?.toSet()
            ?: emptySet()
        panel.setHighlightedSlots(slotIndices)
    }

    private fun navigateToRange(range: TextRange) {
        val editor = textEditor.editor
        editor.caretModel.moveToOffset(range.startOffset)
        editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
        editor.contentComponent.requestFocusInWindow()
    }

    private fun buildComponent(): JComponent {
        val toolbar = ActionManager.getInstance().createActionToolbar(TOOLBAR_PLACE, createToolbarActions(), true)
        val wrapper = JPanel(BorderLayout())
        toolbar.targetComponent = wrapper
        wrapper.add(toolbar.component, BorderLayout.NORTH)
        wrapper.add(JBScrollPane(panel), BorderLayout.CENTER)
        return wrapper
    }

    private fun createToolbarActions(): DefaultActionGroup {
        val group = DefaultActionGroup()
        group.add(object : AnAction("Zoom In", "Increase preview zoom", AllIcons.General.ZoomIn) {
            override fun actionPerformed(e: AnActionEvent) = panel.zoomIn()
            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = panel.canZoomIn()
            }
            override fun getActionUpdateThread() = ActionUpdateThread.EDT
        })
        group.add(object : AnAction("Zoom Out", "Decrease preview zoom", AllIcons.General.ZoomOut) {
            override fun actionPerformed(e: AnActionEvent) = panel.zoomOut()
            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = panel.canZoomOut()
            }
            override fun getActionUpdateThread() = ActionUpdateThread.EDT
        })
        group.add(object : AnAction("Reset Zoom", "Reset preview zoom to 100%", AllIcons.General.ActualZoom) {
            override fun actionPerformed(e: AnActionEvent) = panel.resetZoom()
            override fun getActionUpdateThread() = ActionUpdateThread.EDT
        })
        group.addSeparator()
        group.add(object : ToggleAction("Show Slot Numbers", "Toggle the slot index overlay", AllIcons.General.InlineVariables) {
            override fun isSelected(e: AnActionEvent) = panel.showSlotNumbers
            override fun setSelected(e: AnActionEvent, state: Boolean) {
                panel.showSlotNumbers = state
            }
            override fun getActionUpdateThread() = ActionUpdateThread.EDT
        })
        return group
    }

    override fun getComponent(): JComponent = rootComponent

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
