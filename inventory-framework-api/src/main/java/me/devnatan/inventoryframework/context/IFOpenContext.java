package me.devnatan.inventoryframework.context;

import java.util.concurrent.CompletableFuture;
import me.devnatan.inventoryframework.ViewConfig;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.ViewContainer;
import me.devnatan.inventoryframework.state.AsyncState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * This context is created before the container is opened, it is used for cancellation by previously
 * defined data also can be used to change the title and size of the container before the rendering intent.
 */
public interface IFOpenContext extends IFConfinedContext {

    /**
     * The task that will run before this context transitions from opening to rendering context.
     * <p>
     * <b><i> This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided. </i></b>
     *
     * @return The task that will run before this context gets completed.
     */
    @ApiStatus.Internal
    CompletableFuture<Void> getAsyncOpenJob();

    /**
     * Waits for a task to run before ending this opening context and transitioning to the rendering
     * context.
     * <p>
     * This transition represents the opening of this context's container to the player viewing it.
     *
     * @param task The task that will be waited for.
     */
    void waitUntil(@NotNull CompletableFuture<Void> task);

    /**
     * Waits for a asynchronous state to be complete before ending this opening context and
     * transitioning to the rendering context (waits to show the inventory to the player).
     * <p>
     * <b>This API is experimental and is not subject to the general compatibility guarantees such API
     * may be changed or may be removed completely in any further release.</b>
     *
     * @param state The state to wait before opening the inventory.
     */
    @ApiStatus.Experimental
    void waitFor(@NotNull AsyncState<?> state);

    /**
     * Whether opening the container to the viewer has been cancelled.
     *
     * @return If <code>true</code> the container will not be displayed to the player.
     */
    boolean isCancelled();

    /**
     * Cancels the opening of this context's container to the viewer.
     *
     * @param cancelled If <code>true</code>, the container will not open for the player.
     */
    void setCancelled(boolean cancelled);

    /**
     * Allows access and change the current configuration specifically to that context.
     * <p>
     * By default, all contexts inherit their root configuration but context configuration always
     * takes precedence over root.
     *
     * @return The current context configuration.
     */
    @NotNull
    ViewConfigBuilder modifyConfig();

    /**
     * <b><i> This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided. </i></b>
     */
    @ApiStatus.Internal
    ViewConfig getModifiedConfig();

    /**
     * <b><i> This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided. </i></b>
     */
    @ApiStatus.Internal
    ViewContainer getContainer();

    /**
     * <b><i> This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided. </i></b>
     */
    @ApiStatus.Internal
    void setContainer(ViewContainer container);
}
