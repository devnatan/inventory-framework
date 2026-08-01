package me.devnatan.inventoryframework.bukkit.context;

import me.devnatan.inventoryframework.RootView;
import me.devnatan.inventoryframework.ViewContainer;
import me.devnatan.inventoryframework.Viewer;
import me.devnatan.inventoryframework.component.Component;
import me.devnatan.inventoryframework.context.IFRenderContext;
import me.devnatan.inventoryframework.context.IFSlotClickContext;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SlotClickContext extends SlotContext implements IFSlotClickContext {

    private final Viewer whoClicked;
    private final ViewContainer clickedContainer;
    private final Component clickedComponent;
    private final InventoryClickEvent clickOrigin;
    private final boolean combined;
    private boolean cancelled;

    @ApiStatus.Internal
    public SlotClickContext(
            int slot,
            @NotNull IFRenderContext parent,
            @NotNull Viewer whoClicked,
            @NotNull ViewContainer clickedContainer,
            @Nullable Component clickedComponent,
            @NotNull InventoryClickEvent clickOrigin,
            boolean combined) {
        super(slot, parent);
        this.whoClicked = whoClicked;
        this.clickedContainer = clickedContainer;
        this.clickedComponent = clickedComponent;
        this.clickOrigin = clickOrigin;
        this.combined = combined;
    }

    /**
     * The player who clicked on the slot.
     */
    public final @NotNull Player getPlayer() {
        return (Player) clickOrigin.getWhoClicked();
    }

    /**
     * The event that triggered this context.
     * <p>
     * This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided.
     */
    @NotNull
    public InventoryClickEvent getClickOrigin() {
        return clickOrigin;
    }

    /**
     * The item that was clicked.
     */
    @Override
    public final ItemStack getItem() {
        return clickOrigin.getCurrentItem();
    }

    @Override
    public final Component getComponent() {
        return clickedComponent;
    }

    @Override
    public final @NotNull ViewContainer getClickedContainer() {
        return clickedContainer;
    }

    @Override
    public final boolean isCancelled() {
        return cancelled;
    }

    @Override
    public final void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
        getClickOrigin().setCancelled(cancelled);
    }

    @Override
    public final Object getPlatformEvent() {
        return clickOrigin;
    }

    @Override
    public final int getClickedSlot() {
        return clickOrigin.getRawSlot();
    }

    @Override
    public final boolean isLeftClick() {
        return getClickOrigin().isLeftClick();
    }

    @Override
    public final boolean isRightClick() {
        return getClickOrigin().isRightClick();
    }

    @Override
    public final boolean isMiddleClick() {
        return getClickOrigin().getClick() == ClickType.MIDDLE;
    }

    @Override
    public final boolean isShiftClick() {
        return getClickOrigin().isShiftClick();
    }

    @Override
    public final boolean isKeyboardClick() {
        return getClickOrigin().getClick().isKeyboardClick();
    }

    @Override
    public final boolean isOutsideClick() {
        return getClickOrigin().getSlotType() == InventoryType.SlotType.OUTSIDE;
    }

    @Override
    public final String getClickIdentifier() {
        return getClickOrigin().getClick().name();
    }

    @Override
    public final boolean isOnEntityContainer() {
        return getClickOrigin().getClickedInventory() instanceof PlayerInventory;
    }

    @Override
    public final Viewer getViewer() {
        return whoClicked;
    }

    @Override
    public final void closeForPlayer() {
        getParent().closeForPlayer();
    }

    @Override
    public final void openForPlayer(@NotNull Class<? extends RootView> other) {
        getParent().openForPlayer(other);
    }

    @Override
    public final void openForPlayer(@NotNull Class<? extends RootView> other, Object initialData) {
        getParent().openForPlayer(other, initialData);
    }

    @Override
    public final void updateTitleForPlayer(@NotNull String title) {
        getParent().updateTitleForPlayer(title);
    }

    @Override
    public final void updateTitleForPlayer(@NotNull Object title) {
        getParent().updateTitleForPlayer(title);
    }

    @Override
    public final void resetTitleForPlayer() {
        getParent().resetTitleForPlayer();
    }

    @Override
    public final boolean isCombined() {
        return combined;
    }
}
