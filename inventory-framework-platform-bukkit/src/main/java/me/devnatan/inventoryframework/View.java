package me.devnatan.inventoryframework;

import java.util.Collections;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.CloseContext;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.internal.ElementFactory;
import me.devnatan.inventoryframework.pipeline.CancelledCloseInterceptor;
import me.devnatan.inventoryframework.pipeline.GlobalClickInterceptor;
import me.devnatan.inventoryframework.pipeline.ItemClickInterceptor;
import me.devnatan.inventoryframework.pipeline.ItemCloseOnClickInterceptor;
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

    /**
     * Opens this view instance directly to a player.
     * <p>
     * Unlike {@link ViewFrame#open(Class, Player)}, this does not require the view to be looked up
     * by its class, so it works for views built with the inline builder API as well as regular
     * class-based views.
     *
     * @param player The player that'll see this view.
     * @return The id of the newly created context.
     */
    @ApiStatus.Experimental
    public final String open(@NotNull Player player) {
        return open(player, null);
    }

    /**
     * Opens this view instance directly to a player with initial data.
     * <p>
     * Unlike {@link ViewFrame#open(Class, Player, Object)}, this does not require the view to be
     * looked up by its class, so it works for views built with the inline builder API as well as
     * regular class-based views.
     *
     * @param player      The player that'll see this view.
     * @param initialData The initial data.
     * @return The id of the newly created context.
     */
    @ApiStatus.Experimental
    public final String open(@NotNull Player player, Object initialData) {
        return open(Collections.singletonList(getElementFactory().createViewer(player, null)), initialData);
    }
}
