package me.devnatan.inventoryframework.context;

import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import me.devnatan.inventoryframework.RootView;
import me.devnatan.inventoryframework.ViewConfig;
import me.devnatan.inventoryframework.ViewContainer;
import me.devnatan.inventoryframework.Viewer;
import me.devnatan.inventoryframework.component.ComponentFactory;
import me.devnatan.inventoryframework.internal.LayoutSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

public class MockRenderIFContext extends AbstractIFContext implements IFRenderContext {

    private final RootView root;

    public MockRenderIFContext(RootView root) {
        this.root = root;
    }

    @Override
    public @UnmodifiableView List<ComponentFactory> getComponentFactories() {
        return new ArrayList<>();
    }

    @Override
    public List<LayoutSlot> getLayoutSlots() {
        return new ArrayList<>();
    }

    @Override
    public void addLayoutSlot(@NotNull LayoutSlot layoutSlot) {}

    @Override
    public List<BiFunction<Integer, Integer, ComponentFactory>> getAvailableSlotFactories() {
        return new ArrayList<>();
    }

    @Override
    public Map<Integer, List<BiFunction<Integer, Integer, ComponentFactory>>> getRowSlotFactories() {
        return new HashMap<>();
    }

    @Override
    public Map<Integer, List<BiFunction<Integer, Integer, ComponentFactory>>> getColumnSlotFactories() {
        return new HashMap<>();
    }

    @Override
    public @NotNull ViewContainer getContainer() {
        return mock(ViewContainer.class);
    }

    @Override
    public boolean isRendered() {
        return false;
    }

    @Override
    public Viewer getViewer() {
        return mock(Viewer.class);
    }

    @Override
    public void closeForPlayer() {}

    @Override
    public void openForPlayer(@NotNull Class<? extends RootView> other) {}

    @Override
    public void openForPlayer(@NotNull Class<? extends RootView> other, Object initialData) {}

    @Override
    public void updateTitleForPlayer(@NotNull String title) {}

    @Override
    public void updateTitleForPlayer(@NotNull Object title) {}

    @Override
    public void resetTitleForPlayer() {}

    @Override
    public void back() {}

    @Override
    public void back(Object initialData) {}

    @Override
    public boolean canBack() {
        return false;
    }

    @Override
    public @NotNull UUID getId() {
        return new UUID(3, 4);
    }

    @Override
    public @NotNull ViewConfig getConfig() {
        return mock(ViewConfig.class);
    }

    @Override
    public @NotNull RootView getRoot() {
        return root;
    }

    @Override
    public Object getInitialData() {
        return null;
    }

    @Override
    public void setInitialData(Object initialData) {}

    @Override
    public void updateTitleForEveryone(@NotNull String title) {}

    @Override
    public void updateTitleForEveryone(@NotNull Object title) {}

    @Override
    public void resetTitleForEveryone() {}

    @Override
    public void closeForEveryone() {}

    @Override
    public void openForEveryone(@NotNull Class<? extends RootView> other) {}

    @Override
    public void openForEveryone(@NotNull Class<? extends RootView> other, Object initialData) {}

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public void setActive(boolean active) {}

    @Override
    public boolean isEndless() {
        return false;
    }

    @Override
    public void setEndless(boolean endless) {}
}
