package me.devnatan.inventoryframework.intellij

import com.intellij.ui.JBColor
import java.awt.Color
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
            g.drawString("Unable to analyze this view", ORIGIN_X, 20)
            return
        }

        g.color = JBColor.foreground()
        g.drawString(currentModel.title ?: "(dynamic title)", ORIGIN_X, 20)

        for (row in 0 until currentModel.rows) {
            for (col in 0 until currentModel.columns) {
                if (row * currentModel.columns + col >= currentModel.maxSize) continue
                paintSlot(g, currentModel, row, col)
            }
        }
    }

    private fun paintSlot(g: Graphics, model: PreviewModel, row: Int, col: Int) {
        val x = ORIGIN_X + col * (SLOT_SIZE + SLOT_GAP)
        val y = ORIGIN_Y + row * (SLOT_SIZE + SLOT_GAP)
        val layoutChar = model.layout?.getOrNull(row)?.getOrNull(col)
        val slot = model.slots[row * model.columns + col]

        g.color = when {
            slot?.dynamic == true -> JBColor.YELLOW
            slot?.material != null -> colorForMaterial(slot.material)
            layoutChar != null && layoutChar != ' ' -> JBColor.LIGHT_GRAY
            else -> JBColor.GRAY
        }
        g.fillRect(x, y, SLOT_SIZE, SLOT_SIZE)
        g.color = JBColor.DARK_GRAY
        g.drawRect(x, y, SLOT_SIZE, SLOT_SIZE)

        g.color = Color.BLACK
        when {
            slot?.dynamic == true -> g.drawString("?", x + SLOT_SIZE / 2 - 3, y + SLOT_SIZE / 2 + 5)
            slot?.material != null -> g.drawString(abbreviateMaterial(slot.material), x + 3, y + SLOT_SIZE - 4)
        }
    }

    private fun computePreferredSize(forModel: PreviewModel?): Dimension {
        if (forModel == null) return Dimension(240, 100)
        return Dimension(
            ORIGIN_X * 2 + forModel.columns * (SLOT_SIZE + SLOT_GAP),
            ORIGIN_Y + forModel.rows * (SLOT_SIZE + SLOT_GAP),
        )
    }

    private fun colorForMaterial(material: String): Color {
        val hue = (material.hashCode().and(0xFFFFFF)) % 360 / 360f
        return Color.getHSBColor(hue, 0.45f, 0.85f)
    }

    private fun abbreviateMaterial(material: String): String =
        material.split('_').joinToString("") { it.take(1) }.take(3).uppercase()
}
