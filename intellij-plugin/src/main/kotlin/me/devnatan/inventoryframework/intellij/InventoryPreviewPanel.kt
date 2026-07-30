package me.devnatan.inventoryframework.intellij

import com.intellij.openapi.vfs.VirtualFile
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

class InventoryPreviewPanel(file: VirtualFile) : JPanel(BorderLayout()) {

    init {
        add(JLabel("Inventory view detected: ${file.name}", SwingConstants.CENTER), BorderLayout.CENTER)
    }
}
