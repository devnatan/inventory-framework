package me.devnatan.inventoryframework.intellij

import me.devnatan.inventoryframework.internal.LayoutSlot
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UForExpression
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.visitor.AbstractUastVisitor

private const val FRAMEWORK_PACKAGE_PREFIX = "me.devnatan.inventoryframework"
private const val AVAILABLE_SLOT_METHOD = "availableSlot"

// Mirrors AvailableSlotInterceptor in inventory-framework-core, which can't be called directly:
// it resolves availableSlot(...) calls against a live IFRenderContext built while the user's code
// actually runs, and this plugin never compiles or executes that code - only its call sites are
// known statically. So the two algorithms (resolveFromInitialSlot / resolveFromLayoutSlot) are
// reimplemented here against the plugin's own statically-collected data instead.
internal object AvailableSlotResolver {

    // Every availableSlot(...) call site claims exactly one slot per execution, in registration
    // (i.e. source/loop-iteration) order, regardless of whether it ends up binding an item or a
    // click handler ItemExtractor/ClickHandlerExtractor can recognize - so all call sites must be
    // counted here, not just the ones with a resolvable binding, to keep later calls' assigned
    // slots correct. A call site directly inside a simple bounded counting loop (see
    // ForLoopAnalyzer) is counted once per statically-known iteration - the idiomatic way to
    // batch-fill available slots - rather than once total; anything else (nested loops, for-each,
    // while, non-i++/i-- steps) falls back to counting it once, same as a bare call outside a loop.
    fun collectAnchors(uFile: UFile): List<Int> {
        val anchors = mutableListOf<Int>()
        uFile.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                val method = node.resolve() ?: return false
                val declaringClass = method.containingClass?.qualifiedName ?: return false
                if (node.methodName != AVAILABLE_SLOT_METHOD || !declaringClass.startsWith(FRAMEWORK_PACKAGE_PREFIX)) {
                    return false
                }
                val anchor = SlotTargetResolver.anchorOf(node) ?: return false
                val enclosingLoop = node.getParentOfType<UForExpression>(strict = true)
                val repeats = enclosingLoop?.let { ForLoopAnalyzer.analyze(it)?.values?.size } ?: 1
                repeat(repeats.coerceAtLeast(0)) { anchors += anchor }
                return false
            }
        })
        return anchors
    }

    // Without a layout, slots fill sequentially from 0 (resolveFromInitialSlot); with one, only
    // positions marked with the layout's reserved fill character are candidates
    // (resolveFromLayoutSlot). Either way, slots already claimed by an explicit binding
    // (slot/layoutSlot/row/column) are skipped, and calls beyond the container's capacity are
    // silently dropped rather than shown overflowing - the plugin has no error/diagnostic surface
    // for a preview-time SlotFillExceededException. Returned as a list per anchor because a single
    // call site inside a loop claims several slots, all bound to the same statically-extracted item.
    fun resolve(
        anchors: List<Int>,
        occupiedSlots: Set<Int>,
        layout: List<String>?,
        columns: Int,
        maxSize: Int,
    ): Map<Int, List<Int>> {
        val candidates = if (layout != null) {
            buildList {
                layout.forEachIndexed { row, rowChars ->
                    rowChars.forEachIndexed { col, character ->
                        if (character == LayoutSlot.FILLED_RESERVED_CHAR) add(row * columns + col)
                    }
                }
            }
        } else {
            (0 until maxSize).toList()
        }.filterNot { it in occupiedSlots }

        val resolved = mutableMapOf<Int, MutableList<Int>>()
        anchors.forEachIndexed { i, anchor ->
            candidates.getOrNull(i)?.let { resolved.getOrPut(anchor) { mutableListOf() }.add(it) }
        }
        return resolved
    }
}
