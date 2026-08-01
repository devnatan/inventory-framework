package me.devnatan.inventoryframework.minestom.pipeline

import me.devnatan.inventoryframework.VirtualView
import me.devnatan.inventoryframework.component.ItemComponent
import me.devnatan.inventoryframework.minestom.context.SlotClickContext
import me.devnatan.inventoryframework.pipeline.PipelineContext
import me.devnatan.inventoryframework.pipeline.PipelineInterceptor
import net.minestom.server.event.inventory.InventoryPreClickEvent

/** Intercepted when a player clicks on an item the view container. */
class ItemClickInterceptor : PipelineInterceptor<VirtualView> {
    override fun intercept(
        pipeline: PipelineContext<VirtualView>,
        subject: VirtualView,
    ) {
        if (subject !is SlotClickContext) return

        val event: InventoryPreClickEvent = subject.clickOrigin
        event.inventory ?: return

        val component = subject.component ?: return

        if (component is ItemComponent) {
            val item: ItemComponent = component

            // inherit cancellation so we can un-cancel it
            subject.isCancelled = item.isCancelOnClick
        }
    }
}
