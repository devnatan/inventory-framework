package me.devnatan.inventoryframework.minestom

import me.devnatan.inventoryframework.PlatformView
import me.devnatan.inventoryframework.VirtualView
import me.devnatan.inventoryframework.component.MinestomItemComponentBuilder
import me.devnatan.inventoryframework.minestom.context.CloseContext
import me.devnatan.inventoryframework.minestom.context.Context
import me.devnatan.inventoryframework.minestom.context.OpenContext
import me.devnatan.inventoryframework.minestom.context.RenderContext
import me.devnatan.inventoryframework.minestom.context.SlotClickContext
import me.devnatan.inventoryframework.minestom.pipeline.CancelledCloseInterceptor
import me.devnatan.inventoryframework.minestom.pipeline.GlobalClickInterceptor
import me.devnatan.inventoryframework.minestom.pipeline.ItemClickInterceptor
import me.devnatan.inventoryframework.minestom.pipeline.ItemCloseOnClickInterceptor
import me.devnatan.inventoryframework.pipeline.Pipeline
import me.devnatan.inventoryframework.pipeline.StandardPipelinePhases
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import org.jetbrains.annotations.ApiStatus.OverrideOnly

/** Minestom platform [PlatformView] implementation. */
@OverrideOnly
open class View :
    PlatformView<
        ViewFrame,
        Player,
        MinestomItemComponentBuilder,
        Context,
        OpenContext,
        CloseContext,
        RenderContext,
        SlotClickContext,
    >() {
    public override fun registerPlatformInterceptors() {
        val pipeline: Pipeline<in VirtualView> = pipeline
        pipeline.intercept(StandardPipelinePhases.CLICK, ItemClickInterceptor())
        pipeline.intercept(StandardPipelinePhases.CLICK, GlobalClickInterceptor())
        pipeline.intercept(StandardPipelinePhases.CLICK, ItemCloseOnClickInterceptor())
        pipeline.intercept(StandardPipelinePhases.CLOSE, CancelledCloseInterceptor())
    }

    override fun nextTick(task: Runnable) {
        MinecraftServer.getSchedulerManager().scheduleNextTick(task)
    }
}
