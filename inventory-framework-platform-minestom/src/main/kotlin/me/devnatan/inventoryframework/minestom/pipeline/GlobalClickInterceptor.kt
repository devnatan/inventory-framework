package me.devnatan.inventoryframework.minestom.pipeline

import me.devnatan.inventoryframework.ViewConfig
import me.devnatan.inventoryframework.VirtualView
import me.devnatan.inventoryframework.minestom.context.SlotClickContext
import me.devnatan.inventoryframework.pipeline.PipelineContext
import me.devnatan.inventoryframework.pipeline.PipelineInterceptor
import net.minestom.server.event.inventory.InventoryPreClickEvent

/**
 * Intercepted when a player clicks on the view container. If the click is canceled, this
 * interceptor ends the pipeline immediately.
 */
class GlobalClickInterceptor : PipelineInterceptor<VirtualView> {
    override fun intercept(
        pipeline: PipelineContext<VirtualView>,
        subject: VirtualView,
    ) {
        if (subject !is SlotClickContext) return

        val event: InventoryPreClickEvent = subject.clickOrigin

        // inherit cancellation so we can un-cancel it
        subject.isCancelled =
            event.isCancelled ||
            subject.config.isOptionSet(ViewConfig.CANCEL_ON_CLICK, true)
        subject.root.onClick(subject)
    }
}
