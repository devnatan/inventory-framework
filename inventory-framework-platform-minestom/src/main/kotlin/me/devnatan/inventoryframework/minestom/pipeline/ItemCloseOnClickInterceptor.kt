package me.devnatan.inventoryframework.minestom.pipeline

import me.devnatan.inventoryframework.VirtualView
import me.devnatan.inventoryframework.component.ItemComponent
import me.devnatan.inventoryframework.minestom.context.SlotClickContext
import me.devnatan.inventoryframework.pipeline.PipelineContext
import me.devnatan.inventoryframework.pipeline.PipelineInterceptor
import net.minestom.server.event.inventory.InventoryPreClickEvent

/**
 * Intercepted when a player clicks on an item the view container. Checks if the container should be
 * closed when the item is clicked.
 */
class ItemCloseOnClickInterceptor : PipelineInterceptor<VirtualView> {
    override fun intercept(
        pipeline: PipelineContext<VirtualView>,
        subject: VirtualView,
    ) {
        if (subject !is SlotClickContext) return

        val event: InventoryPreClickEvent = subject.clickOrigin
        event.inventory ?: return

        val component = subject.component
        if (component !is ItemComponent || !component.isVisible) return

        val item: ItemComponent = component as ItemComponent
        if (item.isCloseOnClick) {
            subject.closeForPlayer()
            pipeline.finish()
        }
    }
}
