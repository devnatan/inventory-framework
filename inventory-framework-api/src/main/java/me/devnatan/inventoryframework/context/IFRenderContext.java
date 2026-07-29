package me.devnatan.inventoryframework.context;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import me.devnatan.inventoryframework.ViewContainer;
import me.devnatan.inventoryframework.component.ComponentFactory;
import me.devnatan.inventoryframework.internal.LayoutSlot;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

public interface IFRenderContext extends IFConfinedContext {

    /**
     * <b><i> This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided. </i></b>
     */
    @UnmodifiableView
    @ApiStatus.Internal
    List<ComponentFactory> getComponentFactories();

    /**
     * <b><i> This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided. </i></b>
     */
    @ApiStatus.Internal
    List<LayoutSlot> getLayoutSlots();

    /**
     * <b><i> This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided. </i></b>
     */
    @ApiStatus.Internal
    void addLayoutSlot(@NotNull LayoutSlot layoutSlot);

    /**
     * <b><i> This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided. </i></b>
     */
    @ApiStatus.Internal
    List<BiFunction<Integer, Integer, ComponentFactory>> getAvailableSlotFactories();

    /**
     * <b><i> This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided. </i></b>
     */
    @ApiStatus.Internal
    Map<Integer, List<BiFunction<Integer, Integer, ComponentFactory>>> getRowSlotFactories();

    /**
     * <b><i> This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided. </i></b>
     */
    @ApiStatus.Internal
    Map<Integer, List<BiFunction<Integer, Integer, ComponentFactory>>> getColumnSlotFactories();

    /**
     * The container of this context.
     * <p>
     * The container is where all the changes that are displayed to the user are applied.
     * <p>
     * Direct modifications to the container must launch an inventory modification error, which
     * signals that that function will change the container for whoever is seeing what, which, if it
     * is not possible at that moment or if the container is not sufficiently prepared for this,
     * it must fail.
     *
     * @return The container of this context.
     */
    @NotNull
    ViewContainer getContainer();

    /**
     * <b><i> This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided. </i></b>
     */
    @ApiStatus.Internal
    boolean isRendered();
}
