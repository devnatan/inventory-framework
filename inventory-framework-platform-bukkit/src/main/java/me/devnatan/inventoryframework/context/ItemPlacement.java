package me.devnatan.inventoryframework.context;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Common contract for placing items at specific slots.
 * <p>
 * Implemented by {@link RenderContext}, for immediate placement during rendering, and by
 * {@code ViewBuilder} (in the {@code inventory-framework-inline} module), for placement declared
 * ahead of render time. What placing an item returns differs per implementation - the item builder
 * itself for further per-item configuration on {@link RenderContext}, versus the implementer for
 * continued top-level chaining on a builder.
 *
 * @param <R> What placing an item returns.
 */
public interface ItemPlacement<R> {

    /**
     * Places an item in a specific slot.
     *
     * @param slot The slot in which the item will be positioned.
     * @param item The item.
     * @return See implementation.
     */
    R slot(int slot, @Nullable ItemStack item);

    /**
     * Places an item at a specific row and column.
     *
     * @param row    The row (Y) in which the item will be positioned.
     * @param column The column (X) in which the item will be positioned.
     * @param item   The item.
     * @return See implementation.
     */
    R slot(int row, int column, @Nullable ItemStack item);

    /**
     * Places an item in the first slot of the container.
     *
     * @param item The item.
     * @return See implementation.
     */
    R firstSlot(@Nullable ItemStack item);

    /**
     * Places an item in the last slot of the container.
     *
     * @param item The item.
     * @return See implementation.
     */
    R lastSlot(@Nullable ItemStack item);

    /**
     * Places an item in every slot matching a layout character.
     *
     * @param character The layout character target.
     * @param item      The item.
     * @return See implementation.
     */
    R layoutSlot(char character, @Nullable ItemStack item);
}
