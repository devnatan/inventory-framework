package me.devnatan.inventoryframework;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import me.devnatan.inventoryframework.context.EndlessContextInfo;
import me.devnatan.inventoryframework.context.IFContext;
import me.devnatan.inventoryframework.pipeline.Pipeline;
import me.devnatan.inventoryframework.pipeline.PipelinePhase;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

public abstract class IFViewFrame<S extends IFViewFrame<S, V>, V extends PlatformView<S, ?, ?, ?, ?, ?, ?, ?>> {

    /**
     * Called when a {@link IFViewFrame} is registered.
     */
    public static final PipelinePhase FRAME_REGISTERED = new PipelinePhase("frame-registered");

    /**
     * Called when a {@link IFViewFrame} is unregistered.
     */
    public static final PipelinePhase FRAME_UNREGISTERED = new PipelinePhase("frame-unregistered");

    private boolean registered;
    protected final Map<UUID, V> registeredViews = new HashMap<>();
    protected final Map<String, Viewer> viewerById = new HashMap<>();
    protected Consumer<ViewConfigBuilder> defaultConfig;

    protected boolean debugEnabled;

    @SuppressWarnings("rawtypes")
    private final Pipeline<IFViewFrame> pipeline = new Pipeline<>(FRAME_REGISTERED, FRAME_UNREGISTERED);

    protected IFViewFrame() {}

    /**
     * <b><i> This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided. </i></b>
     */
    @ApiStatus.Internal
    @SuppressWarnings("rawtypes")
    public final @NotNull Pipeline<IFViewFrame> getPipeline() {
        return pipeline;
    }

    /**
     * All registered views.
     *
     * @return A Map containing all registered views in this view frame.
     */
    protected final @NotNull @UnmodifiableView Map<UUID, V> getRegisteredViews() {
        return Collections.unmodifiableMap(registeredViews);
    }

    /**
     * Gets an instance of a registered view from its class-type.
     *
     * @param type The view type.
     * @return An RootView instance of the given type.
     * @throws IllegalArgumentException If the given cannot be found.
     */
    public final @NotNull V getRegisteredViewByType(@NotNull Class<?> type) {
        return getRegisteredViews().values().stream()
                .filter(view -> view.getClass().equals(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("View not registered: %s", type)));
    }

    /**
     * Registers a new view to this view frame.
     *
     * @param views The views that'll be registered.
     * @return This platform view frame.
     */
    @SuppressWarnings("unchecked")
    @Contract("_ -> this")
    public final S with(V... views) {
        synchronized (registeredViews) {
            for (final V view : views) {
                if (registeredViews.containsKey(view.getUniqueId()))
                    throw new IllegalStateException(String.format(
                            "View %s already registered. Maybe your are using #register() before #with(...).",
                            view.getClass().getName()));

                IFDebug.debug(
                        "Registered view \"%s\" identified as %s",
                        view.getUniqueId(), view.getClass().getSimpleName());
                registeredViews.put(view.getUniqueId(), view);
            }
        }
        return (S) this;
    }

    /**
     * Removes a view from this view frame.
     *
     * @param views The views that'll be removed.
     */
    public final void remove(V... views) {
        synchronized (registeredViews) {
            for (final V view : views) {
                view.closeForEveryone();
                registeredViews.remove(view.getUniqueId());
                IFDebug.debug("Removed view %s", view.getUniqueId());
            }
        }
    }

    /**
     * Registers this view frame.
     *
     * @return This platform view frame.
     * @throws IllegalStateException If this view frame is already registered.
     */
    public abstract S register();

    /**
     * Unregisters this view frame and closes all registered views.
     */
    public abstract void unregister();

    /**
     * If this view frame is registered.
     *
     * @return {@code true} if it's registered or {@code false} otherwise.
     */
    public final boolean isRegistered() {
        return registered;
    }

    /**
     * Sets the registered state of this view frame.
     *
     * @param registered {@code true} to mark this view frame as registered.
     */
    protected final void setRegistered(boolean registered) {
        this.registered = registered;
    }

    /**
     * Opens a view to more than one player with initial data.
     * <p>
     * These players will see the same inventory and share the same context.
     *
     * @param viewClass   The target view to be opened.
     * @param viewers     The viewers that the view will be open to.
     * @param initialData The initial data.
     * @return The id of the newly created {@link IFContext}.
     */
    protected final String internalOpen(
            @NotNull Class<? extends V> viewClass, @NotNull Map<String, ?> viewers, Object initialData) {
        final V view = getRegisteredViewByType(viewClass);
        final List<Viewer> convertedViewers = viewers.values().stream()
                .map(o -> view.getElementFactory().createViewer(o, null))
                .collect(Collectors.toCollection(ArrayList::new));

        convertedViewers.removeIf(viewer -> viewerById.containsKey(viewer.getId()));

        if (convertedViewers.isEmpty()) return null;

        return view.open(convertedViewers, initialData);
    }

    /**
     * Opens an active context.
     *
     * @param viewClass		The target view to be opened.
     * @param contextId 	Context id to open into.
     * @param viewer 		The viewer to attach into the specified context.
     * @param initialData 	The initial viewer data.
     */
    protected final void internalOpenActiveContext(
            Class<? extends RootView> viewClass, String contextId, Object viewer, Object initialData) {
        final V view = getRegisteredViewByType(viewClass);
        final Viewer convertedViewer = view.getElementFactory().createViewer(viewer, null);
        view.open(contextId, convertedViewer, initialData);
    }

    /**
     * Creates a context that is never invalidated.
     * <p>
     * <b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     *
     * @param viewClass The view of the context to be later opened.
     * @return The id of the context.
     */
    @ApiStatus.Experimental
    public final EndlessContextInfo createEndlessContext(@NotNull Class<? extends RootView> viewClass) {
        return createEndlessContext(viewClass, null);
    }

    /**
     * Creates a context that is never invalidated.
     * <p>
     * <b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     *
     * @param viewClass The view of the context to be later opened.
     * @param initialData Initial setup data to pass to opening handler.
     * @return The id of the context.
     */
    @ApiStatus.Experimental
    public final EndlessContextInfo createEndlessContext(
            @NotNull Class<? extends RootView> viewClass, Object initialData) {
        final V view = getRegisteredViewByType(viewClass);
        final String context = view.createEndless(initialData);
        return new EndlessContextInfo(context, view);
    }

    /**
     * <b><i> This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided. </i></b>
     */
    @ApiStatus.Internal
    public void addViewer(@NotNull Viewer viewer) {
        synchronized (viewerById) {
            viewerById.put(viewer.getId(), viewer);
        }
        IFDebug.debug("Viewer added globally %s", viewer.getId());
    }

    void removeViewer(@NotNull Viewer viewer) {
        synchronized (viewerById) {
            viewerById.remove(viewer.getId());
        }
        IFDebug.debug("Viewer removed globally %s", viewer.getId());
    }

    /**
     * Sets the default configuration that will be used for all views registered from this framework.
     * <p>
     * <b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     *
     * @return This framework instance.
     * @see <a href="https://github.com/DevNatan/inventory-framework/wiki/basic-usage#default-configuration">Default Configuration on Wiki</a>
     */
    @SuppressWarnings("unchecked")
    @ApiStatus.Experimental
    public final S defaultConfig(@NotNull Consumer<ViewConfigBuilder> defaultConfig) {
        this.defaultConfig = defaultConfig;
        return (S) this;
    }

    /**
     * The default configuration applier of this framework.
     *
     * @return The default configuration applier of this framework.
     */
    final Consumer<ViewConfigBuilder> getDefaultConfig() {
        return defaultConfig;
    }

    /**
     * Enables internal code debugs.
     * <p>
     * <b><i> This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided. </i></b>
     *
     * @return Enable InventoryFramework debugs.
     */
    @SuppressWarnings("unchecked")
    @ApiStatus.Internal
    public final S enableDebug() {
        this.debugEnabled = true;
        return (S) this;
    }
}
