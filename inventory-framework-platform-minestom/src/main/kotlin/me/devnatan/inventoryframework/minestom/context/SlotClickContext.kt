package me.devnatan.inventoryframework.minestom.context

import me.devnatan.inventoryframework.RootView
import me.devnatan.inventoryframework.ViewContainer
import me.devnatan.inventoryframework.Viewer
import me.devnatan.inventoryframework.component.Component
import me.devnatan.inventoryframework.context.IFRenderContext
import me.devnatan.inventoryframework.context.IFSlotClickContext
import net.minestom.server.entity.Player
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.inventory.PlayerInventory
import net.minestom.server.inventory.click.Click
import net.minestom.server.item.ItemStack
import org.jetbrains.annotations.ApiStatus

class SlotClickContext
    @ApiStatus.Internal
    constructor(
        slot: Int,
        parent: IFRenderContext,
        private val whoClicked: Viewer,
        private val clickedContainer: ViewContainer,
        private val clickedComponent: Component?,
        val clickOrigin: InventoryPreClickEvent,
        private val combined: Boolean,
    ) : SlotContext(slot, parent),
        IFSlotClickContext {
        private var cancelled = false

        override val player: Player
            /** The player who clicked on the slot. */
            get() = clickOrigin.player

        override val item: ItemStack
            /** The item that was clicked. */
            get() = clickOrigin.clickedItem

        val click: Click
            /** The click type. */
            get() = clickOrigin.click

        override fun getComponent(): Component? = clickedComponent

        override fun getClickedContainer(): ViewContainer = clickedContainer

        override fun isCancelled(): Boolean = cancelled

        override fun setCancelled(cancelled: Boolean) {
            this.cancelled = cancelled
            clickOrigin.isCancelled = cancelled
        }

        override fun getPlatformEvent(): Any = clickOrigin

        override fun getClickedSlot(): Int = clickOrigin.slot

        override fun isLeftClick(): Boolean = clickOrigin.click is Click.Left

        override fun isRightClick(): Boolean = clickOrigin.click is Click.Right

        override fun isShiftLeftClick(): Boolean = clickOrigin.click is Click.LeftShift

        override fun isShiftRightClick(): Boolean = clickOrigin.click is Click.RightShift

        override fun isMiddleClick(): Boolean = clickOrigin.click is Click.Middle

        override fun isShiftClick(): Boolean {
            val clickType = clickOrigin.click
            return clickType is Click.LeftShift || clickType is Click.RightShift
        }

        override fun isKeyboardClick(): Boolean = clickOrigin.click is Click.HotbarSwap

        override fun isOutsideClick(): Boolean = clickOrigin.slot < 0

        override fun isOnEntityContainer(): Boolean = clickOrigin.inventory is PlayerInventory

        override fun getViewer(): Viewer = whoClicked

        override fun closeForPlayer() {
            parent.closeForPlayer()
        }

        override fun openForPlayer(other: Class<out RootView?>) {
            parent.openForPlayer(other)
        }

        override fun openForPlayer(
            other: Class<out RootView?>,
            initialData: Any,
        ) {
            parent.openForPlayer(other, initialData)
        }

        override fun updateTitleForPlayer(title: String) {
            parent.updateTitleForPlayer(title)
        }

        override fun updateTitleForPlayer(title: Any) {
            parent.updateTitleForPlayer(title)
        }

        override fun resetTitleForPlayer() {
            parent.resetTitleForPlayer()
        }

        override fun isCombined(): Boolean = combined
    }
