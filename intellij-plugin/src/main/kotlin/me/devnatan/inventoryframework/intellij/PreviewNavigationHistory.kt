package me.devnatan.inventoryframework.intellij

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

// Bridges the interactive-preview "click to open another view" simulation across the file editors
// involved, since each view's preview lives in its own InventoryPreviewFileEditor instance with no
// direct reference to the others. Two things need to survive the jump from one file's editor to
// another's: interactive mode staying on, and a way for "Undo" in the destination to mean "go back
// to where I came from" once there's no more local state to undo.
@Service(Service.Level.PROJECT)
class PreviewNavigationHistory {

    private val activeEditors = mutableMapOf<VirtualFile, InventoryPreviewFileEditor>()

    // Target file -> source file, for a navigation recorded before the target's editor exists yet
    // (e.g. the view being opened has no tab open at all). Consumed once the editor for that file
    // is actually constructed.
    private val pendingArrivals = mutableMapOf<VirtualFile, VirtualFile>()

    fun register(file: VirtualFile, editor: InventoryPreviewFileEditor) {
        activeEditors[file] = editor
    }

    fun unregister(file: VirtualFile, editor: InventoryPreviewFileEditor) {
        if (activeEditors[file] === editor) activeEditors.remove(file)
    }

    // Called just before simulating an "open view" click navigates away from `source` to `target`.
    // If `target`'s editor is already alive (an existing tab being reused), it's told directly;
    // otherwise the navigation is stashed for that editor to pick up once it's created.
    fun recordOpenViewNavigation(source: VirtualFile, target: VirtualFile) {
        val existing = activeEditors[target]
        if (existing != null) {
            existing.onArrivedViaInteractiveNavigation(source)
        } else {
            pendingArrivals[target] = source
        }
    }

    fun consumePendingArrival(target: VirtualFile): VirtualFile? = pendingArrivals.remove(target)

    companion object {
        fun getInstance(project: Project): PreviewNavigationHistory = project.getService(PreviewNavigationHistory::class.java)
    }
}
