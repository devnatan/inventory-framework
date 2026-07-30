package me.devnatan.inventoryframework.intellij

import com.intellij.openapi.util.TextRange
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.JPanel

private const val SLOT_SIZE = 32
private const val SLOT_GAP = 2
private const val ORIGIN_X = 12
private const val ORIGIN_Y = 36

private const val CHEST_TYPE_NAME = "CHEST"
private const val SPRITE_SCALE = 2
private const val SPRITE_SLOT_SIZE = 18
private const val SPRITE_ORIGIN_X = 7
private const val SPRITE_ORIGIN_Y = 17

private val chestSprites: Map<Int, BufferedImage?> by lazy {
    (1..6).associateWith { rows ->
        InventoryPreviewPanel::class.java.getResourceAsStream("/assets/sprites/chest-$rows.png")?.use(ImageIO::read)
    }
}

class InventoryPreviewPanel : JPanel() {

    private var model: PreviewModel? = null
    private var highlightedSlotIndices: Set<Int> = emptySet()

    var onSlotClicked: ((TextRange) -> Unit)? = null

    init {
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val currentModel = model ?: return
                val index = slotIndexAt(currentModel, e.point) ?: return
                val range = currentModel.slots[index]?.sourceRange ?: return
                onSlotClicked?.invoke(range)
            }
        })
    }

    fun setModel(newModel: PreviewModel?) {
        model = newModel
        highlightedSlotIndices = emptySet()
        preferredSize = computePreferredSize(newModel)
        revalidate()
        repaint()
    }

    fun setHighlightedSlots(indices: Set<Int>) {
        if (highlightedSlotIndices == indices) return
        highlightedSlotIndices = indices
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

        val sprite = chestSpriteFor(currentModel)
        if (sprite != null) paintSpriteGrid(g, currentModel, sprite) else paintDrawnGrid(g, currentModel)
    }

    private fun chestSpriteFor(model: PreviewModel): BufferedImage? {
        if (model.viewTypeName != CHEST_TYPE_NAME) return null
        return chestSprites[model.rows.coerceIn(1, 6)]
    }

    // Shared by painting and click hit-testing so the two can never drift apart.
    private fun slotGeometry(model: PreviewModel): Triple<Int, Int, Int> {
        val sprite = chestSpriteFor(model)
        return if (sprite != null) {
            Triple(ORIGIN_X + SPRITE_ORIGIN_X * SPRITE_SCALE, ORIGIN_Y + SPRITE_ORIGIN_Y * SPRITE_SCALE, SPRITE_SLOT_SIZE * SPRITE_SCALE)
        } else {
            Triple(ORIGIN_X, ORIGIN_Y, SLOT_SIZE + SLOT_GAP)
        }
    }

    private fun slotIndexAt(model: PreviewModel, point: Point): Int? {
        val (originX, originY, slotSize) = slotGeometry(model)
        val col = (point.x - originX) / slotSize
        val row = (point.y - originY) / slotSize
        if (point.x < originX || point.y < originY) return null
        if (col !in 0 until model.columns || row !in 0 until model.rows) return null
        val index = row * model.columns + col
        return index.takeIf { it < model.maxSize }
    }

    private fun paintSpriteGrid(g: Graphics, model: PreviewModel, sprite: BufferedImage) {
        (g as Graphics2D).setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
        g.drawImage(sprite, ORIGIN_X, ORIGIN_Y, sprite.width * SPRITE_SCALE, sprite.height * SPRITE_SCALE, null)

        val (slotOriginX, slotOriginY, slotSize) = slotGeometry(model)
        forEachSlot(model) { row, col ->
            val x = slotOriginX + col * slotSize
            val y = slotOriginY + row * slotSize
            paintSlotOverlay(g, model, row, col, x, y, slotSize, paintEmptyBackground = false)
        }
    }

    private fun paintDrawnGrid(g: Graphics, model: PreviewModel) {
        val (originX, originY, slotSize) = slotGeometry(model)
        forEachSlot(model) { row, col ->
            val x = originX + col * slotSize
            val y = originY + row * slotSize
            paintSlotOverlay(g, model, row, col, x, y, SLOT_SIZE, paintEmptyBackground = true)
        }
    }

    private fun forEachSlot(model: PreviewModel, action: (row: Int, col: Int) -> Unit) {
        for (row in 0 until model.rows) {
            for (col in 0 until model.columns) {
                if (row * model.columns + col >= model.maxSize) continue
                action(row, col)
            }
        }
    }

    private fun paintSlotOverlay(
        g: Graphics,
        model: PreviewModel,
        row: Int,
        col: Int,
        x: Int,
        y: Int,
        size: Int,
        paintEmptyBackground: Boolean,
    ) {
        val layoutChar = model.layout?.getOrNull(row)?.getOrNull(col)
        val index = row * model.columns + col
        val slot = model.slots[index]
        val isFilled = slot?.dynamic == true || slot?.material != null || (layoutChar != null && layoutChar != ' ')

        if (isFilled || paintEmptyBackground) {
            g.color = when {
                slot?.dynamic == true -> JBColor.YELLOW
                slot?.material != null -> colorForMaterial(slot.material)
                layoutChar != null && layoutChar != ' ' -> JBColor.LIGHT_GRAY
                else -> JBColor.GRAY
            }
            g.fillRect(x + 1, y + 1, size - 2, size - 2)
            if (paintEmptyBackground) {
                g.color = JBColor.DARK_GRAY
                g.drawRect(x, y, size, size)
            }
        }

        g.color = Color.BLACK
        when {
            slot?.dynamic == true -> g.drawString("?", x + size / 2 - 3, y + size / 2 + 5)
            slot?.material != null -> g.drawString(abbreviateMaterial(slot.material), x + 3, y + size - 4)
        }

        if (index in highlightedSlotIndices) {
            g.color = JBColor.BLUE
            g.drawRect(x, y, size - 1, size - 1)
            g.drawRect(x + 1, y + 1, size - 3, size - 3)
        }
    }

    private fun computePreferredSize(forModel: PreviewModel?): Dimension {
        if (forModel == null) return Dimension(240, 100)

        val sprite = chestSpriteFor(forModel)
        if (sprite != null) {
            return Dimension(ORIGIN_X * 2 + sprite.width * SPRITE_SCALE, ORIGIN_Y + sprite.height * SPRITE_SCALE)
        }
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
