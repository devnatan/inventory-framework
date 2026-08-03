package me.devnatan.inventoryframework.intellij

import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
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
private const val MIN_MARGIN = 12
// Padding between the title's baseline and the grid below it; the space actually reserved
// above the grid also includes the title font's real ascent+descent (see nonSpriteTopReserve).
private const val TITLE_GRID_GAP = 6

// Vanilla Minecraft's default font renders glyphs about 7px tall unscaled; scaled up to match
// the sprite grid it reads close to how the game actually looks, rather than a generic UI size.
private const val BASE_TITLE_FONT_SIZE = 9f
private const val BASE_SLOT_NUMBER_FONT_SIZE = 8f

// Vanilla Minecraft draws every container's title at (8, 6) relative to the top-left corner
// of its GUI texture, left-aligned rather than centered.
private const val TITLE_INSET_X = 8
private const val TITLE_INSET_Y = 6

private const val CHEST_TYPE_NAME = "CHEST"
private const val SPRITE_SCALE = 2
private const val SPRITE_SLOT_SIZE = 18
private const val SPRITE_ORIGIN_X = 7
private const val SPRITE_ORIGIN_Y = 17
private const val TITLE_FONT_SIZE = BASE_TITLE_FONT_SIZE * SPRITE_SCALE
private const val SLOT_NUMBER_FONT_SIZE = BASE_SLOT_NUMBER_FONT_SIZE * SPRITE_SCALE - 2f

// Vanilla Minecraft renders container titles in a fixed dark gray (0x404040) regardless of
// any theme, since it's part of the emulated game screen rather than IDE chrome.
private val TITLE_COLOR = Color(0x40, 0x40, 0x40)

private val ZOOM_LEVELS = listOf(0.5, 0.75, 1.0, 1.25, 1.5, 2.0, 3.0)
private val DEFAULT_ZOOM_INDEX = ZOOM_LEVELS.indexOf(1.0)

private val SLOT_NUMBER_BACKGROUND = Color(0, 0, 0, 170)

private val chestSprites: Map<Int, BufferedImage?> by lazy {
    (1..6).associateWith { rows ->
        InventoryPreviewPanel::class.java.getResourceAsStream("/assets/sprites/chest-$rows.png")?.use(ImageIO::read)
    }
}

// Mirrors the Minecraft default font's blocky look; null if the resource is missing or the
// platform rejects the font file, in which case callers fall back to the panel's default font.
private val monocraftFont: Font? by lazy {
    runCatching {
        InventoryPreviewPanel::class.java.getResourceAsStream("/assets/fonts/monocraft/Monocraft.otf")?.use {
            Font.createFont(Font.TRUETYPE_FONT, it)
        }
    }.getOrNull()
}

private val titleFont: Font? by lazy { monocraftFont?.deriveFont(Font.PLAIN, TITLE_FONT_SIZE) }
private val slotNumberFont: Font? by lazy { monocraftFont?.deriveFont(Font.PLAIN, SLOT_NUMBER_FONT_SIZE) }

class InventoryPreviewPanel : JPanel() {

    private var model: PreviewModel? = null
    private var highlightedSlotIndices: Set<Int> = emptySet()
    private var zoomIndex = DEFAULT_ZOOM_INDEX
    private val zoom: Double get() = ZOOM_LEVELS[zoomIndex]

    var onSlotClicked: ((Int) -> Unit)? = null

    var showSlotNumbers: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            repaint()
        }

    var showEmptySlots: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            repaint()
        }

    init {
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val currentModel = model ?: return
                val logicalPoint = Point((e.x / zoom).toInt(), (e.y / zoom).toInt())
                val index = slotIndexAt(currentModel, logicalPoint) ?: return
                onSlotClicked?.invoke(index)
            }
        })
    }

    fun setModel(newModel: PreviewModel?) {
        model = newModel
        highlightedSlotIndices = emptySet()
        refreshSize()
        repaint()
    }

    fun hasModel(): Boolean = model != null

    fun setHighlightedSlots(indices: Set<Int>) {
        if (highlightedSlotIndices == indices) return
        highlightedSlotIndices = indices
        repaint()
    }

    fun canZoomIn(): Boolean = zoomIndex < ZOOM_LEVELS.lastIndex

    fun canZoomOut(): Boolean = zoomIndex > 0

    fun zoomIn() {
        if (!canZoomIn()) return
        zoomIndex++
        onZoomChanged()
    }

    fun zoomOut() {
        if (!canZoomOut()) return
        zoomIndex--
        onZoomChanged()
    }

    fun resetZoom() {
        if (zoomIndex == DEFAULT_ZOOM_INDEX) return
        zoomIndex = DEFAULT_ZOOM_INDEX
        onZoomChanged()
    }

    private fun onZoomChanged() {
        refreshSize()
        repaint()
    }

    private fun refreshSize() {
        preferredSize = computePreferredSize(model)
        revalidate()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        (g as Graphics2D).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.scale(zoom, zoom)
        val currentModel = model
        if (currentModel == null) {
            g.color = JBColor.foreground()
            g.drawString("Unable to analyze this view", MIN_MARGIN, 20)
            return
        }

        val (originX, originY) = gridOrigin(currentModel)
        val sprite = chestSpriteFor(currentModel)
        if (sprite != null) {
            paintSpriteGrid(g, currentModel, sprite, originX, originY)
        } else {
            paintDrawnGrid(g, currentModel, originX, originY)
        }
        paintTitle(g, currentModel, originX, originY)
    }

    // Drawn after the grid/frame so the text sits on top of it, matching the in-game
    // layering where the title is part of the container's foreground, not a separate header.
    private fun paintTitle(g: Graphics, model: PreviewModel, originX: Int, originY: Int) {
        val title = model.title ?: "(dynamic title)"
        g.color = TITLE_COLOR
        val originalFont = g.font
        titleFont?.let { g.font = it }
        val (x, y) = if (chestSpriteFor(model) != null) {
            (originX + TITLE_INSET_X * SPRITE_SCALE) to (originY + TITLE_INSET_Y * SPRITE_SCALE - TITLE_GRID_GAP + g.fontMetrics.ascent)
        } else {
            (originX + TITLE_INSET_X) to (originY - TITLE_GRID_GAP - g.fontMetrics.descent)
        }
        g.drawString(title, x, y)
        g.font = originalFont
    }

    private fun chestSpriteFor(model: PreviewModel): BufferedImage? {
        if (model.viewTypeName != CHEST_TYPE_NAME) return null
        return chestSprites[model.rows.coerceIn(1, 6)]
    }

    private fun gridContentSize(model: PreviewModel): Dimension {
        val sprite = chestSpriteFor(model)
        return if (sprite != null) {
            Dimension(sprite.width * SPRITE_SCALE, sprite.height * SPRITE_SCALE)
        } else {
            Dimension(model.columns * (SLOT_SIZE + SLOT_GAP), model.rows * (SLOT_SIZE + SLOT_GAP))
        }
    }

    // Centers the grid within whatever space is currently available, falling back to a fixed
    // margin when the panel is smaller than the content. Recomputed from the panel's live
    // width/height on every call (paint and click hit-testing alike) so they can never drift apart.
    private fun gridOrigin(model: PreviewModel): Pair<Int, Int> {
        val size = gridContentSize(model)
        val (marginX, marginY) = logicalOrigin(model)
        // width/height are actual on-screen pixels, already inflated by zoom (see
        // computePreferredSize); divide back down since painting happens in a scaled transform.
        val logicalWidth = (width / zoom).toInt()
        val logicalHeight = (height / zoom).toInt()
        val x = maxOf(marginX, (logicalWidth - size.width) / 2)
        val y = maxOf(marginY, (logicalHeight - size.height) / 2)
        return x to y
    }

    // The top-left corner the grid/title would sit at with no extra space to center within,
    // i.e. the tight bounds used for a cropped export. The sprite frame already reserves room
    // for the title in its own texture, so only the frame-less fallback needs the extra top gap.
    private fun logicalOrigin(model: PreviewModel): Pair<Int, Int> {
        val topReserve = if (chestSpriteFor(model) != null) MIN_MARGIN else nonSpriteTopReserve()
        return MIN_MARGIN to topReserve
    }

    // The panel's content size at zoom = 1, with no viewport slack included. Used both to size
    // the component's preferred size (scaled by zoom) and to size a tightly-cropped export.
    private fun logicalContentSize(model: PreviewModel): Dimension {
        val size = gridContentSize(model)
        val (_, topReserve) = logicalOrigin(model)
        return Dimension(MIN_MARGIN * 2 + size.width, topReserve + size.height)
    }

    // Sized from the title font's real ascent/descent rather than a guessed constant, so the
    // reserved band always fits the title regardless of font metrics.
    private fun nonSpriteTopReserve(): Int {
        val fm = getFontMetrics(titleFont ?: font)
        return MIN_MARGIN + fm.ascent + fm.descent + TITLE_GRID_GAP
    }

    // Shared by painting and click hit-testing so the two can never drift apart.
    private fun slotGeometry(model: PreviewModel, originX: Int, originY: Int): Triple<Int, Int, Int> {
        val sprite = chestSpriteFor(model)
        return if (sprite != null) {
            Triple(originX + SPRITE_ORIGIN_X * SPRITE_SCALE, originY + SPRITE_ORIGIN_Y * SPRITE_SCALE, SPRITE_SLOT_SIZE * SPRITE_SCALE)
        } else {
            Triple(originX, originY, SLOT_SIZE + SLOT_GAP)
        }
    }

    private fun slotIndexAt(model: PreviewModel, point: Point): Int? {
        val (originX, originY) = gridOrigin(model)
        val (slotOriginX, slotOriginY, slotSize) = slotGeometry(model, originX, originY)
        if (point.x < slotOriginX || point.y < slotOriginY) return null
        val col = (point.x - slotOriginX) / slotSize
        val row = (point.y - slotOriginY) / slotSize
        if (col !in 0 until model.columns || row !in 0 until model.rows) return null
        val index = row * model.columns + col
        return index.takeIf { it < model.maxSize }
    }

    private fun paintSpriteGrid(g: Graphics, model: PreviewModel, sprite: BufferedImage, originX: Int, originY: Int) {
        (g as Graphics2D).setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
        g.drawImage(sprite, originX, originY, sprite.width * SPRITE_SCALE, sprite.height * SPRITE_SCALE, null)

        val (slotOriginX, slotOriginY, slotSize) = slotGeometry(model, originX, originY)
        forEachSlot(model) { row, col ->
            val x = slotOriginX + col * slotSize
            val y = slotOriginY + row * slotSize
            paintSlotOverlay(g, model, row, col, x, y, slotSize, paintEmptyBackground = false)
        }
    }

    private fun paintDrawnGrid(g: Graphics, model: PreviewModel, originX: Int, originY: Int) {
        val (slotOriginX, slotOriginY, slotSize) = slotGeometry(model, originX, originY)
        forEachSlot(model) { row, col ->
            val x = slotOriginX + col * slotSize
            val y = slotOriginY + row * slotSize
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
        val isFilled = slot?.dynamic == true || slot?.material != null
        // A layout character with no resolved binding (e.g. a pagination content-area marker)
        // isn't "filled" - we just don't know what's actually rendered there - so it's still
        // subject to the empty-slot toggle like any other unfilled slot.
        val isLayoutPlaceholder = !isFilled && layoutChar != null && layoutChar != ' '
        // Only resolves to a real texture if the user has a local Minecraft client jar installed
        // (see ItemIconProvider); otherwise null and the colored-square placeholder below is used.
        val icon = slot?.material?.let { ItemIconProvider.iconFor(it) }

        if (isFilled || ((paintEmptyBackground || isLayoutPlaceholder) && showEmptySlots)) {
            if (icon == null) {
                g.color = when {
                    slot?.dynamic == true -> JBColor.YELLOW
                    slot?.material != null -> colorForMaterial(slot.material)
                    isLayoutPlaceholder -> JBColor.LIGHT_GRAY
                    else -> JBColor.GRAY
                }
                g.fillRect(x + 1, y + 1, size - 2, size - 2)
            }
            if (paintEmptyBackground) {
                g.color = JBColor.DARK_GRAY
                g.drawRect(x, y, size, size)
            }
        }

        if (icon != null) {
            (g as Graphics2D).setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
            val inset = size / 16
            g.drawImage(icon, x + inset, y + inset, size - inset * 2, size - inset * 2, null)
        }

        g.color = Color.BLACK
        when {
            slot?.dynamic == true -> g.drawString("?", x + size / 2 - 3, y + size / 2 + 5)
            slot?.material != null && icon == null -> g.drawString(abbreviateMaterial(slot.material), x + 3, y + size - 4)
        }

        if (index in highlightedSlotIndices) {
            g.color = JBColor.BLUE
            g.drawRect(x, y, size - 1, size - 1)
            g.drawRect(x + 1, y + 1, size - 3, size - 3)
        }

        if (showSlotNumbers) {
            val label = index.toString()
            val originalFont = g.font
            g.font = slotNumberFont ?: g.font.deriveFont(SLOT_NUMBER_FONT_SIZE)
            val fm = g.fontMetrics
            g.color = SLOT_NUMBER_BACKGROUND
            g.fillRect(x + 1, y + 1, fm.stringWidth(label) + 4, fm.ascent + 3)
            g.color = Color.WHITE
            g.drawString(label, x + 3, y + fm.ascent + 1)
            g.font = originalFont
        }
    }

    private fun computePreferredSize(forModel: PreviewModel?): Dimension {
        if (forModel == null) return Dimension(240, 100)
        val logical = logicalContentSize(forModel)
        return Dimension((logical.width * zoom).toInt(), (logical.height * zoom).toInt())
    }

    // Renders just the title + grid, cropped tightly to their content and with a fully
    // transparent background, for exporting (e.g. "copy as image") independent of the current
    // on-screen zoom or however much extra viewport space the panel happens to occupy.
    fun renderContentImage(): BufferedImage? {
        val currentModel = model ?: return null
        val size = logicalContentSize(currentModel)
        val image = BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            val (originX, originY) = logicalOrigin(currentModel)
            val sprite = chestSpriteFor(currentModel)
            if (sprite != null) {
                paintSpriteGrid(g, currentModel, sprite, originX, originY)
            } else {
                paintDrawnGrid(g, currentModel, originX, originY)
            }
            paintTitle(g, currentModel, originX, originY)
        } finally {
            g.dispose()
        }
        return image
    }

    private fun colorForMaterial(material: String): Color {
        val hue = (material.hashCode().and(0xFFFFFF)) % 360 / 360f
        return Color.getHSBColor(hue, 0.45f, 0.85f)
    }

    private fun abbreviateMaterial(material: String): String =
        material.split('_').joinToString("") { it.take(1) }.take(3).uppercase()
}
