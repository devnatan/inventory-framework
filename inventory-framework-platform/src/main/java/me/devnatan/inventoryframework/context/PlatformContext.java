package me.devnatan.inventoryframework.context;

import me.devnatan.inventoryframework.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PlatformContext extends AbstractIFContext {

    private boolean endless;
    private boolean active = true;

    protected PlatformContext() {}

    @SuppressWarnings("rawtypes")
    @Override
    public abstract @NotNull PlatformView getRoot();

    /**
     * Tries to get a container from the current context or throws an exception if not available.
     * @return The container of this context.
     * @throws InventoryFrameworkException If there's no container available in the current context.
     */
    protected final @NotNull ViewContainer getContainerOrThrow() {
        if (this instanceof IFRenderContext) return ((IFRenderContext) this).getContainer();
        if (this instanceof IFCloseContext) return ((IFCloseContext) this).getContainer();
        if (this instanceof IFSlotContext) return ((IFSlotContext) this).getContainer();

        throw new InventoryFrameworkException(String.format(
                "Container is not available in the current context: %s",
                getClass().getName()));
    }

    /**
     * The actual title of this context.
     * <p>
     * If the title has been dynamically changed, it will return the {@link #getUpdatedTitle() updated title}.
     *
     * @return The updated title, the current title of this view, if <code>null</code> will return
     * the default title for this view type.
     */
    @NotNull
    public String getTitle() {
        return getUpdatedTitle() == null ? getInitialTitle() : getUpdatedTitle();
    }

    /**
     * Title that has been {@link #updateTitleForEveryone(String) dynamically changed} in this context.
     *
     * @return The updated title or null if it wasn't updated.
     * @see #updateTitleForEveryone(String)
     */
    @Nullable
    public final String getUpdatedTitle() {
        return getContainerOrThrow().getTitle();
    }

    @Override
    public final void updateTitleForEveryone(@NotNull String title) {
        for (final Viewer viewer : getViewers()) getContainerOrThrow().changeTitle(title, viewer);
    }

    @Override
    public void updateTitleForEveryone(@NotNull Object title) {
        for (final Viewer viewer : getViewers()) getContainerOrThrow().changeTitle(title, viewer);
    }

    @Override
    public final void resetTitleForEveryone() {
        for (final Viewer viewer : getViewers()) getContainerOrThrow().changeTitle(null, viewer);
    }

    @Override
    public final void closeForEveryone() {
        getContainerOrThrow().close();
    }

    @Override
    public final void openForEveryone(@NotNull Class<? extends RootView> other) {
        openForEveryone(other, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void openForEveryone(@NotNull Class<? extends RootView> other, Object initialData) {
        getRoot().navigateTo(other, (IFRenderContext) this, initialData);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean isEndless() {
        return endless;
    }

    @Override
    public void setEndless(boolean endless) {
        this.endless = endless;
    }

    @Override
    public String toString() {
        return "PlatformContext{" + "endless=" + endless + ", active=" + active + "} " + super.toString();
    }
}
