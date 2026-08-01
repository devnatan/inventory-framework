package me.devnatan.inventoryframework.bukkit;

import me.devnatan.inventoryframework.PlatformView;
import me.devnatan.inventoryframework.VirtualView;
import me.devnatan.inventoryframework.bukkit.context.CloseContext;
import me.devnatan.inventoryframework.bukkit.context.Context;
import me.devnatan.inventoryframework.bukkit.context.OpenContext;
import me.devnatan.inventoryframework.bukkit.context.RenderContext;
import me.devnatan.inventoryframework.bukkit.context.SlotClickContext;
import me.devnatan.inventoryframework.bukkit.pipeline.CancelledCloseInterceptor;
import me.devnatan.inventoryframework.bukkit.pipeline.GlobalClickInterceptor;
import me.devnatan.inventoryframework.bukkit.pipeline.ItemClickInterceptor;
import me.devnatan.inventoryframework.bukkit.pipeline.ItemCloseOnClickInterceptor;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.internal.ElementFactory;
import me.devnatan.inventoryframework.pipeline.Pipeline;
import me.devnatan.inventoryframework.pipeline.StandardPipelinePhases;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Bukkit platform {@link PlatformView} implementation.
 */
@ApiStatus.OverrideOnly
public class View
        extends PlatformView<
                ViewFrame,
                Player,
                BukkitItemComponentBuilder,
                Context,
                OpenContext,
                CloseContext,
                RenderContext,
                SlotClickContext> {

    @Override
    public final @NotNull ElementFactory getElementFactory() {
        return super.getElementFactory();
    }

    @Override
    public final void registerPlatformInterceptors() {
        final Pipeline<? super VirtualView> pipeline = getPipeline();
        pipeline.intercept(StandardPipelinePhases.CLICK, new ItemClickInterceptor());
        pipeline.intercept(StandardPipelinePhases.CLICK, new GlobalClickInterceptor());
        pipeline.intercept(StandardPipelinePhases.CLICK, new ItemCloseOnClickInterceptor());
        pipeline.intercept(StandardPipelinePhases.CLOSE, new CancelledCloseInterceptor());
    }

    @Override
    public final void nextTick(Runnable task) {
        Bukkit.getServer().getScheduler().runTask(getFramework().getOwner(), task);
    }
}
