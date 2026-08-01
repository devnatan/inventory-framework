package me.devnatan.inventoryframework.minestom.context

import me.devnatan.inventoryframework.MinestomViewer
import me.devnatan.inventoryframework.RootView
import me.devnatan.inventoryframework.Viewer
import me.devnatan.inventoryframework.component.Component
import me.devnatan.inventoryframework.context.IFRenderContext
import me.devnatan.inventoryframework.context.IFSlotRenderContext
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.UnknownNullability

class SlotRenderContext
    @ApiStatus.Internal
    constructor(
        slot: Int,
        parent: IFRenderContext,
        private val viewer: Viewer?,
        private val _component: Component,
    ) : SlotContext(slot, parent),
        IFSlotRenderContext {
        override val player: Player = (viewer as MinestomViewer).player

        override var item: ItemStack = ItemStack.AIR
        private var cancelled = false
        private var changed = false
        private var forceUpdate = false

        override fun getResult(): ItemStack = item

        override fun isCancelled(): Boolean = cancelled

        override fun setCancelled(cancelled: Boolean) {
            this.cancelled = cancelled
        }

        override fun clear() {
            item = ItemStack.AIR
        }

        override fun hasChanged(): Boolean = changed

        override fun setChanged(changed: Boolean) {
            this.changed = changed
        }

        override fun isForceUpdate(): Boolean = forceUpdate

        override fun setForceUpdate(forceUpdate: Boolean) {
            this.forceUpdate = forceUpdate
        }

        override fun isOnEntityContainer(): Boolean = container.isEntityContainer

        override fun getComponent(): @UnknownNullability Component = _component

        override fun getViewer(): Viewer? = viewer

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
    }
