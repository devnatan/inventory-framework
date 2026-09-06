package me.devnatan.inventoryframework.context;

import org.jetbrains.annotations.ApiStatus;

public interface IFSlotRenderContext extends IFSlotContext, IFConfinedContext {

    @ApiStatus.Internal
    Object getResult();

    /**
     * Sets the item that was rendered by the owning component in a previous render.
     *
     * <p><b><i>This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided.</i></b>
     */
    @ApiStatus.Internal
    void setResult(Object result);

    boolean isCancelled();

    void setCancelled(boolean cancelled);

    /**
     * Clears this slot from the current context.
     * <p>
     * The slot will only be cleaned on the next update, so if you want it cleaned immediately
     * update the slot using {@link #update()}.
     *
     * <p><b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     */
    @ApiStatus.Experimental
    void clear();

    /**
     * Checks if the item in this context has been changed.
     *
     * <p><b><i>This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided.</i></b>
     *
     * @return If the item in this context has been changed.
     */
    @ApiStatus.Internal
    boolean hasChanged();

    /**
     * Marks this context as changed.
     *
     * <p>Improperly changing this property can cause unexpected side effects.
     *
     * <p><b><i>This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided.</i></b>
     *
     * @param changed If the context item was changed.
     */
    @ApiStatus.Internal
    void setChanged(boolean changed);

    /**
     * <b><i> This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided. </i></b>
     */
    @ApiStatus.Internal
    boolean isForceUpdate();

    /**
     * <b><i> This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided. </i></b>
     */
    @ApiStatus.Internal
    void setForceUpdate(boolean forceUpdate);
}
