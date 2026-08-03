package me.devnatan.inventoryframework.intellij

import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.JBColor
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.Rectangle

// Renders the current interactive-preview value as a block line above a state field's
// declaration. Only meaningful while interactive mode is on - see
// InventoryPreviewFileEditor.refreshStateHints. `offset` is the field declaration's start offset,
// used to align the text with the code's actual indentation (tabs/spaces alike) rather than
// starting at column 0, since block elements don't auto-indent themselves.
internal class StateValueInlayRenderer(private val text: String, private val offset: Int) : EditorCustomElementRenderer {

    override fun calcWidthInPixels(inlay: Inlay<*>): Int = indentX(inlay) + metrics(inlay).stringWidth(text) + 6

    override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
        val fm = metrics(inlay)
        g.color = JBColor.GRAY
        g.font = font(inlay)
        g.drawString(text, targetRegion.x + indentX(inlay), targetRegion.y + fm.ascent + (targetRegion.height - fm.height) / 2)
    }

    private fun indentX(inlay: Inlay<*>): Int = inlay.editor.offsetToXY(offset).x

    private fun font(inlay: Inlay<*>): Font = inlay.editor.colorsScheme.getFont(EditorFontType.PLAIN).deriveFont(Font.ITALIC)

    private fun metrics(inlay: Inlay<*>): FontMetrics = inlay.editor.contentComponent.getFontMetrics(font(inlay))
}
