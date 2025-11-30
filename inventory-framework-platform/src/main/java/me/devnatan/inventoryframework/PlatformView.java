package me.devnatan.inventoryframework;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import me.devnatan.inventoryframework.component.*;
import me.devnatan.inventoryframework.context.IFCloseContext;
import me.devnatan.inventoryframework.context.IFConfinedContext;
import me.devnatan.inventoryframework.context.IFContext;
import me.devnatan.inventoryframework.context.IFOpenContext;
import me.devnatan.inventoryframework.context.IFRenderContext;
import me.devnatan.inventoryframework.context.IFSlotClickContext;
import me.devnatan.inventoryframework.context.IFSlotContext;
import me.devnatan.inventoryframework.context.PlatformRenderContext;
import me.devnatan.inventoryframework.internal.ElementFactory;
import me.devnatan.inventoryframework.internal.PlatformUtils;
import me.devnatan.inventoryframework.pipeline.AvailableSlotInterceptor;
import me.devnatan.inventoryframework.pipeline.ComponentClickHandlerCallInterceptor;
import me.devnatan.inventoryframework.pipeline.ContextInvalidationOnCloseInterceptor;
import me.devnatan.inventoryframework.pipeline.FirstRenderInterceptor;
import me.devnatan.inventoryframework.pipeline.LayoutRenderInterceptor;
import me.devnatan.inventoryframework.pipeline.LayoutResolutionInterceptor;
import me.devnatan.inventoryframework.pipeline.Pipeline;
import me.devnatan.inventoryframework.pipeline.PlatformCloseInterceptor;
import me.devnatan.inventoryframework.pipeline.PlatformInitInterceptor;
import me.devnatan.inventoryframework.pipeline.PlatformOpenInterceptor;
import me.devnatan.inventoryframework.pipeline.PlatformRenderInterceptor;
import me.devnatan.inventoryframework.pipeline.PlatformUpdateHandlerInterceptor;
import me.devnatan.inventoryframework.pipeline.ScheduledUpdateStartInterceptor;
import me.devnatan.inventoryframework.pipeline.ScheduledUpdateStopInterceptor;
import me.devnatan.inventoryframework.pipeline.StandardPipelinePhases;
import me.devnatan.inventoryframework.pipeline.UpdateInterceptor;
import me.devnatan.inventoryframework.pipeline.ViewerLastInteractionTrackerInterceptor;
import me.devnatan.inventoryframework.state.InitialDataStateValue;
import me.devnatan.inventoryframework.state.MutableIntState;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import me.devnatan.inventoryframework.state.StateAccess;
import me.devnatan.inventoryframework.state.StateAccessImpl;
import me.devnatan.inventoryframework.state.StateValue;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public abstract class PlatformView<
                TFramework extends IFViewFrame<?, ?>,
                TViewer,
                TItem extends ItemComponentBuilder<TItem, TContext> & ComponentFactory,
                TContext extends IFContext,
                TOpenContext extends IFOpenContext,
                TCloseContext extends IFCloseContext,
                TRenderContext extends IFRenderContext,
                TSlotClickContext extends IFSlotClickContext>
        extends DefaultRootView implements Iterable<TContext>, StateAccess<TContext, TItem> {

    private TFramework framework;
    private boolean initialized;
    private final StateAccess<TContext, TItem> stateAccess =
            new StateAccessImpl<>(this, this::getElementFactory, stateRegistry);

    protected PlatformView() {
        super();
    }

    // region Open & Close
    /**
     * Closes all contexts that are currently active in this view.
     */
    public final void closeForEveryone() {
        getContexts().forEach(IFContext::closeForEveryone);
    }

    /**
     * Setups a endless context.
     *
     * @param initialData Initial data to pass through opening handler.
     * @return The id of the generated context.
     */
    final String createEndless(Object initialData) {
        final IFOpenContext context = getElementFactory().createOpenContext(this, null, new ArrayList<>(), initialData);

        context.setEndless(true);
        getPipeline().execute(StandardPipelinePhases.OPEN, context);
        return context.getId().toString();
    }

    /**
     * Opens this view to one or more viewers.
     *
     * @param viewers     The viewers that'll see this view.
     * @param initialData The initial data.
     * @return The id of the generated context.
     */
    final String open(List<Viewer> viewers, Object initialData) {
        if (!isInitialized()) throw new IllegalStateException("Cannot open a uninitialized view");

        final Viewer subject = viewers.size() == 1 ? viewers.get(0) : null;
        final IFOpenContext context = getElementFactory().createOpenContext(this, subject, viewers, initialData);

        getPipeline().execute(StandardPipelinePhases.OPEN, context);
        return context.getId().toString();
    }

    /**
     * Opens an already active context to a viewer.
     *
     * @param contextId The id of the context.
     * @param viewer The viewer to open the context to.
     * @param initialData Initial data.
     */
    @SuppressWarnings("unchecked")
    final void open(String contextId, Viewer viewer, Object initialData) {
        IFRenderContext targetContext = null;
        for (final IFContext context : getInternalContexts()) {
            if (context.getId().toString().equals(contextId)) {
                targetContext = (IFRenderContext) context;
                break;
            }
        }

        if (targetContext == null) throw new IllegalArgumentException("Context not found: " + contextId);
        if (!targetContext.isActive()) throw new IllegalStateException("Invalidated");

        viewer.setActiveContext(targetContext);
        onViewerAdded((TContext) targetContext, (TViewer) viewer.getPlatformInstance(), initialData);
        targetContext.addViewer(viewer);
        getFramework().addViewer(viewer);
        viewer.open(targetContext.getContainer());
    }
    // endregion

    // region Navigation
    /**
     * <p><b><i>This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided.</i></b>
     */
    @SuppressWarnings("rawtypes")
    @ApiStatus.Internal
    public final void navigateTo(
            @NotNull Class<? extends PlatformView> target, @NotNull IFRenderContext origin, Object initialData) {
        final List<Viewer> viewers = origin.getViewers();
        viewers.forEach(viewer -> {
            viewer.setSwitching(true);
            viewer.setPreviousContext(origin);
        });
        getFramework().getRegisteredViewByType(target).open(viewers, initialData);
    }

    /**
     * <p><b><i>This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided.</i></b>
     */
    @SuppressWarnings("rawtypes")
    @ApiStatus.Internal
    public final void navigateTo(
            @NotNull Class<? extends PlatformView> target,
            @NotNull IFRenderContext origin,
            @NotNull Viewer viewer,
            Object initialData) {
        viewer.setPreviousContext(origin);
        viewer.setSwitching(true);
        getFramework().getRegisteredViewByType(target).open(Collections.singletonList(viewer), initialData);
        viewer.setSwitching(false);
    }

    /**
     * <p><b><i>This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided.</i></b>
     */
    @ApiStatus.Internal
    public final void back(@NotNull Viewer viewer) {
        back(viewer, null);
    }

    /**
     * <p><b><i>This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided.</i></b>
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @ApiStatus.Internal
    public final void back(@NotNull Viewer viewer, Object initialData) {
        final IFRenderContext active = viewer.getActiveContext();
        final IFRenderContext target = viewer.getPreviousContext();
        viewer.setPreviousContext(active);
        viewer.setSwitching(true);

        if (initialData != null) {
            for (final Map.Entry<Long, StateValue> entry :
                    target.getStateValues().entrySet()) {
                final StateValue value = entry.getValue();
                if (!(value instanceof InitialDataStateValue)) continue;

                ((InitialDataStateValue) value).reset();
            }
            target.setInitialData(initialData);
        }

        final PlatformView root = (PlatformView) target.getRoot();
        root.onViewerAdded(target, viewer, initialData);
        target.addViewer(viewer);

        if (!root.hasContext(target.getId())) {
            target.setActive(true);
            root.addContext(target);
        }

        if (target.getViewers().size() == 1) target.getContainer().open(viewer);
        else {
            root.renderContext(target);
        }

        viewer.setActiveContext(target);
        root.onResume(active, target);
        viewer.setSwitching(false);
    }
    // endregion

    /**
     * Creates a new ViewConfigBuilder instance with the default platform configuration.
     * Configuration is inherited from the {@link #getFramework() framework} if available.
     *
     * @return A new ViewConfigBuilder instance.
     */
    public final @NotNull ViewConfigBuilder createConfig() {
        final ViewConfigBuilder configBuilder = new ViewConfigBuilder().type(ViewType.CHEST);
        if (getFramework().getDefaultConfig() != null)
            getFramework().getDefaultConfig().accept(configBuilder);
        return configBuilder;
    }

    // region Contexts
    /**
     * Returns the context that is linked to the specified viewer in this view.
     * <p>
     * <b><i> This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided. </i></b>
     *
     * @param viewer The viewer.
     * @return The context of the viewer in this context.
     * @throws IllegalArgumentException If there's no context linked to the given viewer.
     */
    @ApiStatus.Internal
    public final @NotNull IFContext getContext(@NotNull Viewer viewer) {
        for (final IFContext context : getInternalContexts()) {
            if (context.getIndexedViewers().containsKey(viewer.getId())) return context;
        }

        throw new IllegalArgumentException(format("Unable to get context for %s", viewer));
    }

    /**
     * Returns the context that is linked to the specified viewer in this view.
     * <p>
     * <b><i> This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided. </i></b>
     *
     * @param viewerId The id of the viewer.
     * @return The context of the viewer in this context.
     * @throws IllegalArgumentException If there's no context linked to the given viewer.
     */
    public final @NotNull IFContext getContext(@NotNull String viewerId) {
        for (final IFContext context : getInternalContexts()) {
            if (context.getIndexedViewers().containsKey(viewerId)) return context;
        }

        throw new IllegalArgumentException(format("Unable to get context for %s", viewerId));
    }

    /**
     * Adds a context to this view.
     * <p>
     * <b><i> This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided. </i></b>
     *
     * @param context The context to add.
     */
    @ApiStatus.Internal
    public void addContext(@NotNull TContext context) {
        synchronized (getInternalContexts()) {
            getInternalContexts().add(context);
        }
        IFDebug.debug(
                "Context %s added to [%s] %s",
                context.getId(), context.getClass().getSimpleName(), getClass().getName());
    }

    /**
     * Removes a given context from this view if that context is linked to this view.
     * <p>
     * <b><i> This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided. </i></b>
     *
     * @param context The context to remove.
     */
    @ApiStatus.Internal
    public void removeContext(@NotNull IFContext context) {
        synchronized (getInternalContexts()) {
            getInternalContexts().removeIf(other -> other.getId() == context.getId());
        }
        IFDebug.debug("Context %s removed from %s", context.getId(), getClass().getName());
    }

    /**
     * <b><i> This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided. </i></b>
     */
    @ApiStatus.Internal
    public boolean hasContext(@NotNull UUID id) {
        for (final IFContext context : getInternalContexts()) {
            if (context.getId().equals(id)) return true;
        }
        return false;
    }

    /**
     * Renders a given context in this view.
     * <p>
     * <b><i> This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided. </i></b>
     *
     * @param context The context to render.
     */
    @ApiStatus.Internal
    public void renderContext(@NotNull TRenderContext context) {
        getPipeline().execute(context);
        ((PlatformRenderContext<?, ?>) context).setRendered();

        context.getViewers().forEach(viewer -> {
            if (!context.getContainer().isProxied()) context.getContainer().open(viewer);
        });
        IFDebug.debug("Rendering context %s", context.getId());
    }

    @SuppressWarnings("rawtypes")
    @ApiStatus.Internal
    public void removeAndTryInvalidateContext(@NotNull Viewer viewer, @NotNull TContext context) {
        context.removeViewer(viewer);

        if (!viewer.isSwitching())
            ((PlatformView) context.getRoot()).getFramework().removeViewer(viewer);

        final IFRenderContext target = viewer.getCurrentContext();
        if (target.isEndless()) return;
        if (target.getViewers().size() <= 1) {
            target.setActive(false);
            removeContext(target);
        }
    }

    @Override
    public void invalidateEndlessContext(String contextId) {
        final IFContext context = getInternalContexts().stream()
                .filter(value -> value.getId().toString().equals(contextId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Context not found: " + contextId));

        if (!context.isActive()) return;
        if (!context.isEndless())
            throw new IllegalArgumentException(
                    "#invalidateEndlessContext() can only be called in #isEndless() == true context");

        IFDebug.debug("Invalidating endless context %s...", contextId);

        // closeForEveryone() will perform everything needed to ensure non-abnormal closing
        // like calling close handlers, state management, removing viewers and so on
        context.closeForEveryone();

        // We need to do this here because in the natural flow of context invalidating checks if the
        // context is endless so if it's an endless then it will not be removed nor deactivated
        context.setActive(false);
        removeContext(context);
    }
    // endregion

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public final Iterator<TContext> iterator() {
        return (Iterator<TContext>) getContexts().iterator();
    }

    // region Refs API
    /**
     * Creates a new unassigned reference instance.
     * <p>
     * <b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     *
     * @return A new unassigned {@link Ref} instance.
     * @param <E> Type of the element hold by this reference.
     * @see <a href="https://github.com/DevNatan/inventory-framework/wiki/refs-api">Refs API on Wiki</a>
     */
    @ApiStatus.Experimental
    protected final <E> Ref<E> ref() {
        return new RefImpl<>();
    }

    /**
     * Creates a new empty reference instance that can hold multiple elements of the same type.
     * <p>
     * <b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     *
     * @return A new unassigned {@link Ref} instance.
     * @param <E> Type of the element hold by this reference.
     * @see <a href="https://github.com/DevNatan/inventory-framework/wiki/refs-api">Refs API on Wiki</a>
     */
    @ApiStatus.Experimental
    protected final <E> Ref<List<E>> multiRefs() {
        return new MultiRefsImpl<>();
    }
    // endregion

    // region Public Platform Handlers
    /**
     * Called when the view is about to be configured, the returned object will be the view's
     * configuration.
     *
     * @param config A ViewConfigBuilder instance to configure this view.
     */
    @ApiStatus.OverrideOnly
    public void onInit(@NotNull ViewConfigBuilder config) {}

    /**
     * Called before a context is rendered, used to set up it.
     *
     * <p>This handler is often called "pre-rendering" because it is possible to set the title and
     * size of the inventory and also cancel the opening of the View without even doing any handling
     * related to the inventory.
     *
     * <p>It is not possible to manipulate the inventory in this handler, if it happens an exception
     * will be thrown.
     * <p>
     * <b>This method is called once in Shared Contexts. To know when a viewer is added/removed from
     * this kind of context use {@link #onViewerAdded(TContext, TViewer, Object)}/{@link #onViewerRemoved(TContext, TViewer)}</b>.
     *
     * @param open The open context.
     */
    @ApiStatus.OverrideOnly
    public void onOpen(@NotNull TOpenContext open) {}

    /**
     * Called only once before the container is displayed to a player.
     * <p>
     * The {@code context} is not cancelable, for cancellation use {@link #onOpen(IFOpenContext)} instead.
     * <p>
     * This function should only be used to render items, any external call is completely forbidden
     * as the function runs on the main thread.
     *
     * @param render The rendering context.
     */
    @ApiStatus.OverrideOnly
    public void onFirstRender(@NotNull TRenderContext render) {}

    /**
     * Called when the view is updated for a player.
     *
     * <p>This is a rendering function and can modify the view's inventory.
     *
     * @param update The update context.
     */
    @ApiStatus.OverrideOnly
    public void onUpdate(@NotNull TContext update) {}

    /**
     * Called when the player closes the view's inventory.
     *
     * <p>It is possible to cancel this event and have the view's inventory open again for the player.
     *
     * @param close The player view context.
     */
    @ApiStatus.OverrideOnly
    public void onClose(@NotNull TCloseContext close) {}

    /**
     * Called when an actor clicks on a container while it has a view open.
     * <p>
     * You can know if the click was on entity inventory or view inventory by {@link
     * IFSlotContext#isOnEntityContainer()}
     * Any function that triggers an inventory modification is prohibited from being used in this
     * handler.
     * <p>
     * This context is cancelable and canceling this context will cancel the click, thus canceling
     * all subsequent interceptors causing the pipeline to terminate immediately.
     *
     * @param click The click context.
     */
    @ApiStatus.OverrideOnly
    public void onClick(@NotNull TSlotClickContext click) {}

    /**
     * Called when a context is resumed by {@link IFConfinedContext#back()}.
     * <p>
     * <b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     *
     * @param origin Who called {@link IFConfinedContext#back() to back}.
     * @param target The context that was back to.
     */
    @ApiStatus.OverrideOnly
    @ApiStatus.Experimental
    public void onResume(@NotNull TContext origin, @NotNull TContext target) {}

    /**
     * Called when a {@link Viewer viewer} is added to a context.
     * <p>
     * This method is called after {@link #onFirstRender(IFRenderContext) initial render phase}.
     * <p>
     * <b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     *
     * @param context 	The context.
     * @param viewer 	Who was added to the context.
     * @param data    	Initial data set wen the viewer was added.
     */
    @ApiStatus.OverrideOnly
    @ApiStatus.Experimental
    public void onViewerAdded(@NotNull TContext context, @NotNull TViewer viewer, Object data) {}

    /**
     * Called when a {@link Viewer viewer} is removed from a context.
     * <p>
     * This method is called on {@link #onClose(IFCloseContext) close phase} before context invalidation.
     * <p>
     * <b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     *
     * @param context The context.
     * @param viewer Who was removed from the context.
     */
    @ApiStatus.OverrideOnly
    @ApiStatus.Experimental
    public void onViewerRemoved(@NotNull TContext context, @NotNull TViewer viewer) {}
    // endregion

    // region Internals
    /**
     * <p><b><i>This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided.</i></b>
     */
    @ApiStatus.Internal
    public final TFramework getFramework() {
        return framework;
    }

    /**
     * The initialization state of this view.
     *
     * @return If this view was initialized.
     */
    final boolean isInitialized() {
        return initialized;
    }

    /**
     * Sets the initialization state of this view.
     *
     * @param initialized The new initialization state.
     */
    final void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    /**
     * Throws an exception if this view is already initialized.
     *
     * @throws IllegalStateException if this view is already initialized.
     */
    private void requireNotInitialized() {
        if (!isInitialized()) return;
        throw new IllegalStateException(
                "View is already initialized, please move this method call to class constructor or #onInit.");
    }

    /**
     * Called internally before the first initialization.
     * <p>
     * Use it to register pipeline interceptors.
     *
     * @throws IllegalStateException If this platform view is already initialized.
     */
    @SuppressWarnings("unchecked")
    final void internalInitialization(IFViewFrame<?, ?> framework) {
        if (isInitialized())
            throw new IllegalStateException("Tried to call internal initialization but view is already initialized");

        this.framework = (TFramework) framework;

        final Pipeline<? super VirtualView> pipeline = getPipeline();
        pipeline.intercept(StandardPipelinePhases.INIT, new PlatformInitInterceptor());
        pipeline.intercept(StandardPipelinePhases.OPEN, new PlatformOpenInterceptor());
        pipeline.intercept(StandardPipelinePhases.LAYOUT_RESOLUTION, new LayoutResolutionInterceptor());
        pipeline.intercept(StandardPipelinePhases.FIRST_RENDER, new PlatformRenderInterceptor());
        pipeline.intercept(StandardPipelinePhases.FIRST_RENDER, new LayoutRenderInterceptor());
        pipeline.intercept(StandardPipelinePhases.FIRST_RENDER, new AvailableSlotInterceptor());
        pipeline.intercept(StandardPipelinePhases.FIRST_RENDER, new FirstRenderInterceptor());
        pipeline.intercept(StandardPipelinePhases.VIEWER_ADDED, new ScheduledUpdateStartInterceptor());
        pipeline.intercept(StandardPipelinePhases.VIEWER_REMOVED, new ScheduledUpdateStopInterceptor());
        pipeline.intercept(StandardPipelinePhases.UPDATE, new PlatformUpdateHandlerInterceptor());
        pipeline.intercept(StandardPipelinePhases.UPDATE, new UpdateInterceptor());
        pipeline.intercept(StandardPipelinePhases.CLOSE, new PlatformCloseInterceptor());
        pipeline.intercept(StandardPipelinePhases.CLOSE, new ContextInvalidationOnCloseInterceptor());
        pipeline.intercept(StandardPipelinePhases.CLICK, new ViewerLastInteractionTrackerInterceptor());
        pipeline.intercept(StandardPipelinePhases.CLICK, new ComponentClickHandlerCallInterceptor());
        registerPlatformInterceptors();
        pipeline.execute(StandardPipelinePhases.INIT, this);
    }

    abstract void registerPlatformInterceptors();
    // endregion

    @ApiStatus.Internal
    public @NotNull ElementFactory getElementFactory() {
        return PlatformUtils.getFactory();
    }

    // region State Management
    @Override
    public final <T> State<T> state(T initialValue) {
        requireNotInitialized();
        return stateAccess.state(initialValue);
    }

    @Override
    public final <T> MutableState<T> mutableState(T initialValue) {
        requireNotInitialized();
        return stateAccess.mutableState(initialValue);
    }

    @Override
    public final MutableIntState mutableState(int initialValue) {
        requireNotInitialized();
        return stateAccess.mutableState(initialValue);
    }

    @Override
    public final <T> State<T> computedState(@NotNull Function<TContext, T> computation) {
        requireNotInitialized();
        return stateAccess.computedState(computation);
    }

    @Override
    public final <T> State<T> computedState(@NotNull Supplier<T> computation) {
        requireNotInitialized();
        return stateAccess.computedState(computation);
    }

    @Override
    public final <T> State<T> lazyState(@NotNull Function<TContext, T> computation) {
        requireNotInitialized();
        return stateAccess.lazyState(computation);
    }

    @Override
    public final <T> State<T> lazyState(@NotNull Supplier<T> computation) {
        requireNotInitialized();
        return stateAccess.lazyState(computation);
    }

    @Override
    public <T> State<T> lazyAsyncState(@NotNull Supplier<CompletableFuture<T>> computation) {
        requireNotInitialized();
        return stateAccess.lazyAsyncState(computation);
    }

    @Override
    public <T> State<T> lazyAsyncState(@NotNull Function<TContext, CompletableFuture<T>> computation) {
        requireNotInitialized();
        return stateAccess.lazyAsyncState(computation);
    }

    @Override
    public final <T> MutableState<T> initialState() {
        requireNotInitialized();
        return stateAccess.initialState();
    }

    @Override
    public final <T> MutableState<T> initialState(@NotNull String key) {
        requireNotInitialized();
        return stateAccess.initialState(key);
    }

    @Override
    public final <T> State<Pagination> paginationState(
            @NotNull List<? super T> sourceProvider,
            @NotNull PaginationValueConsumer<TContext, TItem, T> elementConsumer) {
        requireNotInitialized();
        return stateAccess.paginationState(sourceProvider, elementConsumer);
    }

    @Override
    public final <T> State<Pagination> computedPaginationState(
            @NotNull Function<TContext, List<? super T>> sourceProvider,
            @NotNull PaginationValueConsumer<TContext, TItem, T> valueConsumer) {
        requireNotInitialized();
        return stateAccess.computedPaginationState(sourceProvider, valueConsumer);
    }

    @Override
    public final <T> State<Pagination> computedAsyncPaginationState(
            @NotNull Function<TContext, CompletableFuture<List<T>>> sourceProvider,
            @NotNull PaginationValueConsumer<TContext, TItem, T> valueConsumer) {
        requireNotInitialized();
        return stateAccess.computedAsyncPaginationState(sourceProvider, valueConsumer);
    }

    @Override
    public final <T> State<Pagination> lazyPaginationState(
            @NotNull Function<TContext, List<? super T>> sourceProvider,
            @NotNull PaginationValueConsumer<TContext, TItem, T> valueConsumer) {
        requireNotInitialized();
        return stateAccess.lazyPaginationState(sourceProvider, valueConsumer);
    }

    @Override
    public final <T> State<Pagination> lazyPaginationState(
            @NotNull Supplier<List<? super T>> sourceProvider,
            @NotNull PaginationValueConsumer<TContext, TItem, T> valueConsumer) {
        requireNotInitialized();
        return stateAccess.lazyPaginationState(sourceProvider, valueConsumer);
    }

    @Override
    public final <T> State<Pagination> lazyAsyncPaginationState(
            @NotNull Function<TContext, CompletableFuture<List<T>>> sourceProvider,
            @NotNull PaginationValueConsumer<TContext, TItem, T> valueConsumer) {
        requireNotInitialized();
        return stateAccess.lazyAsyncPaginationState(sourceProvider, valueConsumer);
    }

    @Override
    public final <T> PaginationStateBuilder<TContext, TItem, T> buildPaginationState(
            @NotNull List<? super T> sourceProvider) {
        requireNotInitialized();
        return stateAccess.buildPaginationState(sourceProvider);
    }

    @Override
    public final <T> PaginationStateBuilder<TContext, TItem, T> buildComputedPaginationState(
            @NotNull Function<TContext, List<? super T>> sourceProvider) {
        requireNotInitialized();
        return stateAccess.buildComputedPaginationState(sourceProvider);
    }

    @Override
    public final <T> PaginationStateBuilder<TContext, TItem, T> buildComputedAsyncPaginationState(
            @NotNull Function<TContext, CompletableFuture<List<T>>> sourceProvider) {
        requireNotInitialized();
        return stateAccess.buildComputedAsyncPaginationState(sourceProvider);
    }

    @Override
    public final <T> PaginationStateBuilder<TContext, TItem, T> buildLazyPaginationState(
            @NotNull Supplier<List<? super T>> sourceProvider) {
        requireNotInitialized();
        return stateAccess.buildLazyPaginationState(sourceProvider);
    }

    @Override
    public final <T> PaginationStateBuilder<TContext, TItem, T> buildLazyPaginationState(
            @NotNull Function<TContext, List<? super T>> sourceProvider) {
        requireNotInitialized();
        return stateAccess.buildLazyPaginationState(sourceProvider);
    }

    @Override
    public final <T> PaginationStateBuilder<TContext, TItem, T> buildLazyAsyncPaginationState(
            @NotNull Function<TContext, CompletableFuture<List<T>>> sourceProvider) {
        requireNotInitialized();
        return stateAccess.buildLazyAsyncPaginationState(sourceProvider);
    }
    // endregion
}
