package me.devnatan.inventoryframework.bukkit.context;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import me.devnatan.inventoryframework.BukkitViewContainer;
import me.devnatan.inventoryframework.BukkitViewer;
import me.devnatan.inventoryframework.ViewConfig;
import me.devnatan.inventoryframework.ViewContainer;
import me.devnatan.inventoryframework.Viewer;
import me.devnatan.inventoryframework.bukkit.View;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.PlatformRenderContext;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RenderContext extends PlatformRenderContext<BukkitItemComponentBuilder, Context>
        implements Context, InventoryHolder {

    private final Player player;

    /**
     * <b><i> This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided. </i></b>
     */
    @ApiStatus.Internal
    public RenderContext(
            @NotNull UUID id,
            @NotNull View root,
            @NotNull ViewConfig config,
            ViewContainer container,
            @NotNull Map<String, Viewer> viewers,
            Viewer subject,
            Object initialData) {
        super(id, root, config, container, viewers, subject, initialData);
        this.player = subject != null ? ((BukkitViewer) subject).getPlayer() : null;
    }

    @Override
    public @NotNull View getRoot() {
        return (View) root;
    }

    // TODO documentation
    public @NotNull Player getPlayer() {
        tryThrowDoNotWorkWithSharedContext("getAllPlayers");
        return player;
    }

    @Override
    public List<Player> getAllPlayers() {
        return getViewers().stream()
                .map(viewer -> (BukkitViewer) viewer)
                .map(BukkitViewer::getPlayer)
                .collect(Collectors.toList());
    }

    @Override
    public void updateTitleForPlayer(@NotNull String title, @NotNull Player player) {
        ((BukkitViewContainer) getContainer()).changeTitle(title, player);
    }

    @Override
    public void resetTitleForPlayer(@NotNull Player player) {
        ((BukkitViewContainer) getContainer()).changeTitle(null, player);
    }

    /**
     * Adds an item to a specific slot in the context container.
     *
     * @param slot The slot in which the item will be positioned.
     * @return An item builder to configure the item.
     */
    public @NotNull BukkitItemComponentBuilder slot(int slot, @Nullable ItemStack item) {
        return slot(slot).withItem(item);
    }

    /**
     * Adds an item at the specific column and ROW (X, Y) in that context's container.
     *
     * @param row    The row (Y) in which the item will be positioned.
     * @param column The column (X) in which the item will be positioned.
     * @return An item builder to configure the item.
     */
    @NotNull
    public BukkitItemComponentBuilder slot(int row, int column, @Nullable ItemStack item) {
        return slot(row, column).withItem(item);
    }

    /**
     * Sets an item in the first slot of this context's container.
     *
     * @param item The item that'll be set.
     * @return An item builder to configure the item.
     */
    public @NotNull BukkitItemComponentBuilder firstSlot(@Nullable ItemStack item) {
        return firstSlot().withItem(item);
    }

    /**
     * Sets an item in the last slot of this context's container.
     *
     * @param item The item that'll be set.
     * @return An item builder to configure the item.
     */
    public @NotNull BukkitItemComponentBuilder lastSlot(@Nullable ItemStack item) {
        return lastSlot().withItem(item);
    }

    /**
     * Adds an item in the next available slot of this context's container.
     *
     * @param item The item that'll be added.
     * @return An item builder to configure the item.
     */
    public @NotNull BukkitItemComponentBuilder availableSlot(@Nullable ItemStack item) {
        return availableSlot().withItem(item);
    }

    /**
     * Defines the item that will represent a character provided in the context layout.
     *
     * @param character The layout character target.
     * @param item      The item that'll represent the layout character.
     * @return An item builder to configure the item.
     */
    public @NotNull BukkitItemComponentBuilder layoutSlot(char character, @Nullable ItemStack item) {
        return layoutSlot(character).withItem(item);
    }

    /**
     * <p><b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     */
    @ApiStatus.Experimental
    public @NotNull BukkitItemComponentBuilder resultSlot(@Nullable ItemStack item) {
        return resultSlot().withItem(item);
    }

    @Override
    protected BukkitItemComponentBuilder createBuilder() {
        return new BukkitItemComponentBuilder(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RenderContext that = (RenderContext) o;
        return Objects.equals(getPlayer(), that.getPlayer());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getPlayer());
    }

    @Override
    public String toString() {
        return "RenderContext{" + "player=" + player + "} " + super.toString();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return ((BukkitViewContainer) getContainerOrThrow()).getInventory();
    }
}
