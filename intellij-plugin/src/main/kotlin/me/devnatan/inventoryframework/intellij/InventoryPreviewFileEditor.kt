package me.devnatan.inventoryframework.intellij

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.Alarm
import java.awt.BorderLayout
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import javax.swing.JComponent
import javax.swing.JPanel

private const val REFRESH_DEBOUNCE_MILLIS = 300
private const val TOOLBAR_PLACE = "InventoryFramework.PreviewToolbar"
private const val COPY_FEEDBACK_FADEOUT_MILLIS = 1500

private class ResolvedViewClass(val targetFile: VirtualFile?, val navigatable: Navigatable?)

private class ImageTransferable(private val image: Image) : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.imageFlavor)

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == DataFlavor.imageFlavor

    override fun getTransferData(flavor: DataFlavor): Any {
        if (flavor != DataFlavor.imageFlavor) throw UnsupportedFlavorException(flavor)
        return image
    }
}

class InventoryPreviewFileEditor(
    private val project: Project,
    private val file: VirtualFile,
    private val textEditor: TextEditor,
) : UserDataHolderBase(), FileEditor {

    private val panel = InventoryPreviewPanel()
    private val propertyChangeSupport = PropertyChangeSupport(this)
    private val refreshAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)
    private var currentModel: PreviewModel? = null
    private val interactionState = PreviewInteractionState()
    private var interactiveModeEnabled = false
    private val stateInlays = mutableListOf<Inlay<*>>()
    private val rootComponent: JComponent by lazy { buildComponent() }

    // Set when this file's preview was opened by simulating an "open view" click from another
    // view's preview - lets Undo fall back to "go back to that view" once there's no more local
    // interaction state left to undo. See PreviewNavigationHistory.
    private var backNavigationFile: VirtualFile? = null

    init {
        panel.onSlotClicked = ::onSlotClicked
        val navigationHistory = PreviewNavigationHistory.getInstance(project)
        navigationHistory.register(file, this)
        navigationHistory.consumePendingArrival(file)?.let(::onArrivedViaInteractiveNavigation)
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
        interactionState.reset(model)
        panel.setModel(model?.let(interactionState::resolve))
        updateHighlightForCaret()
        refreshStateHints()
    }

    // Shows the current simulated value next to each state field's declaration, e.g.
    // `mutableState(0); → 3`, while interactive mode is on. Rebuilt wholesale on every state
    // change rather than incrementally updated in place - cheap given how few state fields a
    // view typically has, and avoids tracking which inlay belongs to which declaration.
    private fun refreshStateHints() {
        stateInlays.forEach { it.dispose() }
        stateInlays.clear()
        if (!interactiveModeEnabled) return
        val model = currentModel ?: return
        val editor = textEditor.editor
        for (declaration in model.states) {
            val value = interactionState.currentValue(declaration.id) ?: continue
            val renderer = StateValueInlayRenderer("Current value: $value", declaration.declarationOffset)
            editor.inlayModel.addBlockElement(declaration.declarationOffset, false, true, 0, renderer)?.let { stateInlays += it }
        }
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

    private fun onSlotClicked(index: Int) {
        val model = currentModel ?: return
        if (interactiveModeEnabled) {
            simulateClick(model, index)
        } else {
            model.slots[index]?.sourceRange?.let(::navigateToRange)
        }
    }

    private fun simulateClick(model: PreviewModel, index: Int) {
        when (val action = model.clickActions[index]) {
            null -> return
            PreviewClickAction.Unsupported -> showUnsupportedInteractionBalloon()
            is PreviewClickAction.OpenView -> navigateToViewClass(action.targetClassFqn)
            else -> {
                interactionState.apply(action)
                panel.setModel(interactionState.resolve(model))
                refreshStateHints()
                showSimulatedActionBalloon(action)
            }
        }
    }

    // Simulating an actual view switch would mean building and rendering an entirely separate
    // preview model, so the closest useful stand-in for "this click opens another view" is
    // jumping straight to that view's source, mirroring what the click would do at runtime.
    private fun navigateToViewClass(targetClassFqn: String) {
        // findClass/navigationElement/containingFile touch the PSI/stub index, which asserts read
        // access even from the EDT - the mouse-click callback that reaches here doesn't hold one
        // implicitly.
        val resolved = ReadAction.compute<ResolvedViewClass, Throwable> {
            val psiClass = JavaPsiFacade.getInstance(project).findClass(targetClassFqn, GlobalSearchScope.allScope(project))
            ResolvedViewClass(psiClass?.containingFile?.virtualFile, psiClass?.navigationElement as? Navigatable)
        }
        val navigatable = resolved.navigatable
        if (navigatable == null) {
            showViewNotFoundBalloon(targetClassFqn)
            return
        }
        // Recorded before navigating (rather than from the destination editor's init) since the
        // target's tab may already be open, in which case no init ever runs for this jump.
        resolved.targetFile?.let { PreviewNavigationHistory.getInstance(project).recordOpenViewNavigation(file, it) }
        navigatable.navigate(true)
    }

    // Called by PreviewNavigationHistory, either synchronously from navigateToViewClass (target
    // tab already open) or from this editor's own init (target tab just now being created) -
    // either way, arriving here via a simulated "open view" click should carry interactive mode
    // forward and let Undo hop back once there's nothing local left to undo.
    fun onArrivedViaInteractiveNavigation(fromFile: VirtualFile) {
        backNavigationFile = fromFile
        if (interactiveModeEnabled) return
        interactiveModeEnabled = true
        panel.interactiveMode = true
        refreshStateHints()
    }

    private fun showViewNotFoundBalloon(targetClassFqn: String) {
        val simpleName = targetClassFqn.substringAfterLast('.')
        JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder("Could not find view class $simpleName", MessageType.WARNING, null)
            .setFadeoutTime(COPY_FEEDBACK_FADEOUT_MILLIS.toLong())
            .createBalloon()
            .show(RelativePoint.getCenterOf(panel), Balloon.Position.above)
    }

    private fun showSimulatedActionBalloon(action: PreviewClickAction) {
        val (stateId, description) = when (action) {
            is PreviewClickAction.ToggleBoolean -> action.stateId to "toggled"
            is PreviewClickAction.Delta -> action.stateId to "changed by ${if (action.delta >= 0) "+" else ""}${action.delta}"
            is PreviewClickAction.SetLiteral -> action.stateId to "set to ${action.value}"
            is PreviewClickAction.OpenView -> return
            PreviewClickAction.Unsupported -> return
        }
        val fieldName = stateId.substringAfterLast('#')
        JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder("$fieldName $description", MessageType.INFO, null)
            .setFadeoutTime(COPY_FEEDBACK_FADEOUT_MILLIS.toLong())
            .createBalloon()
            .show(RelativePoint.getCenterOf(panel), Balloon.Position.above)
    }

    // Called whenever interactive mode is toggled (on or off) - either direction starts a fresh
    // interactive session, so the "came from" link left over from a previous session's navigation
    // shouldn't carry over into this one.
    private fun resetInteraction() {
        interactionState.reset(currentModel)
        currentModel?.let { panel.setModel(interactionState.resolve(it)) }
        backNavigationFile = null
        refreshStateHints()
    }

    // Local state takes priority: only once there's nothing left to undo in this view does Undo
    // fall back to "go back to the view whose click sent us here", chaining a multi-hop navigation
    // (A opens B opens C) back one step at a time rather than jumping straight to A from C.
    private fun undoLastInteraction() {
        val model = currentModel
        if (model != null && interactionState.undo()) {
            panel.setModel(interactionState.resolve(model))
            refreshStateHints()
            return
        }
        backNavigationFile?.let(::navigateBackToFile)
    }

    private fun navigateBackToFile(target: VirtualFile) {
        if (!target.isValid) return
        OpenFileDescriptor(project, target).navigate(true)
    }

    private fun showUnsupportedInteractionBalloon() {
        JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder("This click handler can't be simulated in the fast preview", MessageType.WARNING, null)
            .setFadeoutTime(COPY_FEEDBACK_FADEOUT_MILLIS.toLong())
            .createBalloon()
            .show(RelativePoint.getCenterOf(panel), Balloon.Position.above)
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
        group.add(object : ToggleAction("Show Empty Slots", "Toggle the placeholder background for empty slots", AllIcons.Graph.Grid) {
            override fun isSelected(e: AnActionEvent) = panel.showEmptySlots
            override fun setSelected(e: AnActionEvent, state: Boolean) {
                panel.showEmptySlots = state
            }
            override fun getActionUpdateThread() = ActionUpdateThread.EDT
        })
        group.addSeparator()
        group.add(object : AnAction("Copy as Image", "Copy the current preview render to the clipboard", AllIcons.Actions.Copy) {
            override fun actionPerformed(e: AnActionEvent) = copyPreviewAsImage(e)
            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = panel.hasModel()
            }
            override fun getActionUpdateThread() = ActionUpdateThread.EDT
        })
        group.addSeparator()
        group.add(
            object : ToggleAction(
                "Interactive Preview",
                "Click slots to simulate their click handlers instead of navigating to source",
                AllIcons.Actions.Execute,
            ) {
                override fun isSelected(e: AnActionEvent) = interactiveModeEnabled
                override fun setSelected(e: AnActionEvent, state: Boolean) {
                    interactiveModeEnabled = state
                    panel.interactiveMode = state
                    resetInteraction()
                }
                override fun update(e: AnActionEvent) {
                    super.update(e)
                    e.presentation.icon = if (interactiveModeEnabled) AllIcons.Actions.Suspend else AllIcons.Actions.Execute
                    e.presentation.text = if (interactiveModeEnabled) "Stop Interactive Preview" else "Start Interactive Preview"
                }
                override fun getActionUpdateThread() = ActionUpdateThread.EDT
            },
        )
        group.add(object : AnAction("Undo Last Interaction", "Revert the last simulated click", AllIcons.Actions.Undo) {
            override fun actionPerformed(e: AnActionEvent) = undoLastInteraction()
            override fun update(e: AnActionEvent) {
                e.presentation.isEnabled = interactiveModeEnabled && (interactionState.canUndo() || backNavigationFile != null)
            }
            override fun getActionUpdateThread() = ActionUpdateThread.EDT
        })
        return group
    }

    private fun copyPreviewAsImage(e: AnActionEvent) {
        val image = panel.renderContentImage() ?: return
        Toolkit.getDefaultToolkit().systemClipboard.setContents(ImageTransferable(image), null)

        val anchor = e.inputEvent?.component as? JComponent ?: panel
        JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder("Copied preview to clipboard", MessageType.INFO, null)
            .setFadeoutTime(COPY_FEEDBACK_FADEOUT_MILLIS.toLong())
            .createBalloon()
            .show(RelativePoint.getSouthOf(anchor), Balloon.Position.below)
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

    override fun dispose() {
        PreviewNavigationHistory.getInstance(project).unregister(file, this)
        stateInlays.forEach { it.dispose() }
        stateInlays.clear()
    }
}
