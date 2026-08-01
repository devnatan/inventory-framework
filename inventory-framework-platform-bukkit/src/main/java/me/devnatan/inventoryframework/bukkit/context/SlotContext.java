package me.devnatan.inventoryframework.bukkit.context;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.devnatan.inventoryframework.ViewConfig;
import me.devnatan.inventoryframework.ViewContainer;
import me.devnatan.inventoryframework.Viewer;
import me.devnatan.inventoryframework.bukkit.View;
import me.devnatan.inventoryframework.component.Component;
import me.devnatan.inventoryframework.context.IFRenderContext;
import me.devnatan.inventoryframework.context.IFSlotContext;
import me.devnatan.inventoryframework.context.PlatformContext;
import me.devnatan.inventoryframework.state.State;
import me.devnatan.inventoryframework.state.StateValue;
import me.devnatan.inventoryframework.state.StateWatcher;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

public abstract class SlotContext extends PlatformContext implements IFSlotContext, Context {

    // --- Inherited ---
    private final IFRenderContext parent;

    // --- Properties ---
    private int slot;

    protected SlotContext(int slot, @NotNull IFRenderContext parent) {
        this.slot = slot;
        this.parent = parent;
    }

    public abstract ItemStack getItem();

    @Override
    public final @NotNull RenderContext getParent() {
        return (RenderContext) parent;
    }

    @Override
    public final int getSlot() {
        return slot;
    }

    @Override
    public final void setSlot(int slot) {
        this.slot = slot;
    }

    @Override
    public final @NotNull Map<String, Viewer> getIndexedViewers() {
        return getParent().getIndexedViewers();
    }

    @Override
    public final @NotNull String getTitle() {
        return getParent().getTitle();
    }

    @Override
    public final @UnmodifiableView @NotNull List<Component> getComponents() {
        return getParent().getComponents();
    }

    @Override
    public List<Component> getInternalComponents() {
        return getParent().getInternalComponents();
    }

    @Override
    public final List<Component> getComponentsAt(int position) {
        return getParent().getComponentsAt(position);
    }

    @Override
    public final void addComponent(@NotNull Component component) {
        getParent().addComponent(component);
    }

    @Override
    public final void removeComponent(@NotNull Component component) {
        getParent().removeComponent(component);
    }

    @Override
    public final void renderComponent(@NotNull Component component) {
        getParent().renderComponent(component);
    }

    @Override
    public final void updateComponent(@NotNull Component component, boolean force) {
        getParent().updateComponent(component, force);
    }

    @Override
    public final void performClickInComponent(
            @NotNull Component component,
            @NotNull Viewer viewer,
            @NotNull ViewContainer clickedContainer,
            Object platformEvent,
            int clickedSlot,
            boolean combined) {
        getParent().performClickInComponent(component, viewer, clickedContainer, platformEvent, clickedSlot, combined);
    }

    @Override
    public final void update() {
        getParent().update();
    }

    @Override
    public final Object getRawStateValue(State<?> state) {
        return getParent().getRawStateValue(state);
    }

    @Override
    public StateValue getInternalStateValue(State<?> state) {
        return getParent().getInternalStateValue(state);
    }

    @Override
    public StateValue getUninitializedStateValue(long stateId) {
        return getParent().getUninitializedStateValue(stateId);
    }

    @Override
    public final void initializeState(long id, @NotNull StateValue value) {
        getParent().initializeState(id, value);
    }

    @Override
    public final void updateState(State<?> state, Object value) {
        getParent().updateState(state, value);
    }

    @Override
    public final void watchState(long id, StateWatcher listener) {
        getParent().watchState(id, listener);
    }

    @Override
    public final @NotNull UUID getId() {
        return getParent().getId();
    }

    @Override
    public final @NotNull ViewConfig getConfig() {
        return getParent().getConfig();
    }

    @Override
    public final @NotNull ViewContainer getContainer() {
        return getParent().getContainer();
    }

    @Override
    public final @NotNull View getRoot() {
        return getParent().getRoot();
    }

    @Override
    public final Object getInitialData() {
        return getParent().getInitialData();
    }

    @Override
    public void setInitialData(Object initialData) {
        getParent().setInitialData(initialData);
    }

    @Override
    public List<Player> getAllPlayers() {
        return getParent().getAllPlayers();
    }

    @Override
    public void updateTitleForPlayer(@NotNull String title, @NotNull Player player) {
        getParent().updateTitleForPlayer(title, player);
    }

    @Override
    public void resetTitleForPlayer(@NotNull Player player) {
        getParent().resetTitleForPlayer(player);
    }

    @Override
    public final boolean isActive() {
        return getParent().isActive();
    }

    @Override
    public final void setActive(boolean active) {
        getParent().setActive(active);
    }

    @Override
    public final boolean isEndless() {
        return getParent().isEndless();
    }

    @Override
    public final void setEndless(boolean endless) {
        getParent().setEndless(endless);
    }

    @Override
    public void back() {
        getParent().back();
    }

    @Override
    public void back(Object initialData) {
        getParent().back(initialData);
    }

    @Override
    public boolean canBack() {
        return getParent().canBack();
    }
}
