package me.devnatan.inventoryframework.bukkit.context;

import me.devnatan.inventoryframework.BukkitViewer;
import me.devnatan.inventoryframework.RootView;
import me.devnatan.inventoryframework.Viewer;
import me.devnatan.inventoryframework.component.Component;
import me.devnatan.inventoryframework.context.IFRenderContext;
import me.devnatan.inventoryframework.context.IFSlotRenderContext;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public class SlotRenderContext extends SlotContext implements IFSlotRenderContext {

    private final Player player;
    private final Viewer viewer;
    private final Component component;
    private ItemStack item;
    private boolean cancelled;
    private boolean changed;
    private boolean forceUpdate;

    @ApiStatus.Internal
    public SlotRenderContext(int slot, @NotNull IFRenderContext parent, @Nullable Viewer viewer, Component component) {
        super(slot, parent);
        this.viewer = viewer;
        this.player = viewer == null ? null : ((BukkitViewer) viewer).getPlayer();
        this.component = component;
    }

    @Override
    public final Component getComponent() {
        return component;
    }

    @Override
    public final @UnknownNullability Player getPlayer() {
        return player;
    }

    @Override
    public final Object getResult() {
        return item;
    }

    @Override
    public final ItemStack getItem() {
        return item;
    }

    public final void setItem(ItemStack item) {
        this.item = item;
        setChanged(true);
    }

    @Override
    public final boolean isCancelled() {
        return cancelled;
    }

    @Override
    public final void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public void clear() {
        setItem(null);
    }

    @Override
    public final boolean hasChanged() {
        return changed;
    }

    @Override
    public final void setChanged(boolean changed) {
        this.changed = changed;
    }

    @Override
    public boolean isForceUpdate() {
        return forceUpdate;
    }

    @Override
    public void setForceUpdate(boolean forceUpdate) {
        this.forceUpdate = forceUpdate;
    }

    @Override
    public final boolean isOnEntityContainer() {
        return getContainer().isEntityContainer();
    }

    @Override
    public final Viewer getViewer() {
        return viewer;
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
}
