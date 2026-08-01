package me.devnatan.inventoryframework.minestom.pipeline

import me.devnatan.inventoryframework.VirtualView
import me.devnatan.inventoryframework.minestom.context.CloseContext
import me.devnatan.inventoryframework.pipeline.PipelineContext
import me.devnatan.inventoryframework.pipeline.PipelineInterceptor

class CancelledCloseInterceptor : PipelineInterceptor<VirtualView> {
    override fun intercept(
        pipeline: PipelineContext<VirtualView>,
        subject: VirtualView,
    ) {
        if (subject !is CloseContext) return

        if (!subject.isCancelled) return

        subject.root.nextTick { subject.viewer.open(subject.container) }
    }
}
