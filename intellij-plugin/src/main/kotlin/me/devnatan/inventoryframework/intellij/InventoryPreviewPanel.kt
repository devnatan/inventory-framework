package me.devnatan.inventoryframework.intellij

import com.intellij.ui.JBColor
import java.awt.Dimension
import java.awt.Graphics
import javax.swing.JPanel

private const val SLOT_SIZE = 32
private const val SLOT_GAP = 2
private const val ORIGIN_X = 12
private const val ORIGIN_Y = 36

class InventoryPreviewPanel : JPanel() {

    private var model: PreviewModel? = null

    fun setModel(newModel: PreviewModel?) {
        model = newModel
        preferredSize = computePreferredSize(newModel)
        revalidate()
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val currentModel = model
        if (currentModel == null) {
            g.color = JBColor.foreground()
            g.drawString("No inventory view detected", ORIGIN_X, 20)
            return
        }

        g.color = JBColor.foreground()
        g.drawString(currentModel.title ?: "(dynamic title)", ORIGIN_X, 20)

        for (row in 0 until currentModel.rows) {
            for (col in 0 until currentModel.columns) {
                val x = ORIGIN_X + col * (SLOT_SIZE + SLOT_GAP)
                val y = ORIGIN_Y + row * (SLOT_SIZE + SLOT_GAP)
                val layoutChar = currentModel.layout?.getOrNull(row)?.getOrNull(col)
                g.color = if (layoutChar != null && layoutChar != ' ') JBColor.LIGHT_GRAY else JBColor.GRAY
                g.fillRect(x, y, SLOT_SIZE, SLOT_SIZE)
                g.color = JBColor.DARK_GRAY
                g.drawRect(x, y, SLOT_SIZE, SLOT_SIZE)
            }
        }
    }

    private fun computePreferredSize(forModel: PreviewModel?): Dimension {
        if (forModel == null) return Dimension(240, 100)
        return Dimension(
            ORIGIN_X * 2 + forModel.columns * (SLOT_SIZE + SLOT_GAP),
            ORIGIN_Y + forModel.rows * (SLOT_SIZE + SLOT_GAP),
        )
    }
}
