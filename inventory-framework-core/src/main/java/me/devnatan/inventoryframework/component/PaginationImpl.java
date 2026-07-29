package me.devnatan.inventoryframework.component;

import static me.devnatan.inventoryframework.IFDebug.debug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import me.devnatan.inventoryframework.Ref;
import me.devnatan.inventoryframework.ViewContainer;
import me.devnatan.inventoryframework.VirtualView;
import me.devnatan.inventoryframework.context.*;
import me.devnatan.inventoryframework.internal.LayoutSlot;
import me.devnatan.inventoryframework.state.AbstractStateValue;
import me.devnatan.inventoryframework.state.State;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import org.jetbrains.annotations.VisibleForTesting;

// TODO add "key" to child pagination components and check if it needs to be updated based on it
@VisibleForTesting
public class PaginationImpl extends AbstractStateValue implements Pagination, InteractionHandler {

    private final String key = UUID.randomUUID().toString();
    private List<Component> components = new ArrayList<>();
    private final IFContext host;
    private boolean visible;

    // --- User provided ---
    private final char layoutTarget;
    private final Object sourceProvider;
    private final PaginationElementFactory<Object> elementFactory;
    private final BiConsumer<IFContext, Pagination> pageSwitchHandler;
    private Pagination.Orientation orientation;

    // --- Internal ---
    private int currPageIndex;
    private final boolean isLazy, isStatic, isComputed, isAsync;
    private boolean pageWasChanged;
    private boolean initialized;
    private int pagesCount;
    private boolean forceUpdated;
    private LayoutSlot currentLayoutSlot;

    // Number of elements that each page can have. -1 means uninitialized.
    private int pageSize = -1;

    // Changes when dynamic data source is used and being loaded
    private boolean isLoading;

    /**
     * Final source factory for dynamic or asynchronous pagination converted from {@link #sourceProvider}.
     * <p>
     * The return parameter of this source factory can be either {@code List} in dynamic pagination
     * or {@code CompletableFuture<List>} in asynchronous pagination.
     */
    private Function<IFContext, Object> _srcFactory;

    // Current page source, null before first pagination render.
    private List<?> currSource;

    public PaginationImpl(
            State<?> state,
            IFContext host,
            char layoutTarget,
            Object sourceProvider,
            PaginationElementFactory<Object> elementFactory,
            BiConsumer<IFContext, Pagination> pageSwitchHandler,
            boolean isAsync,
            boolean isComputed,
            Pagination.Orientation orientation) {
        super(state);
        this.host = host;
        this.layoutTarget = layoutTarget;
        this.sourceProvider = sourceProvider;
        this.elementFactory = elementFactory;
        this.pageSwitchHandler = pageSwitchHandler;
        this.currSource = convertSourceProvider();
        this.isComputed = isComputed;
        this.isAsync = isAsync;
        this.isStatic = sourceProvider instanceof Collection;
        this.isLazy =
                !isStatic && !isComputed && (sourceProvider instanceof Function || sourceProvider instanceof Supplier);
        this.orientation = orientation;
    }

    /**
     * Tries to access and load the source to the current page.
     * <p>
     * If this pagination {@link #isLazy() is lazy} it tries to get the current data source
     * dynamically or asynchronously and waits for its completion. For static pagination it returns
     * immediately with the source.
     * <p>
     * On asynchronous pagination the source update job will be inherited by the user provided one
     * and when job gets done the {@link #currSource} is updated with the result of the computation.
     *
     * @return A CompletableFuture with the current pagination source as result.
     * @throws IllegalStateException In static pagination when the current source wasn't yet defined.
     */
    private CompletableFuture<List<?>> loadSourceForTheCurrentPage() {
        /*
         * In lazy pagination **that was already initialized (already rendered before)** we must
         * use the current data source as source of truth to ensure that page switching do not
         * re-trigger pagination data factory since it will always return the source as a whole,
         * the original one, and not the source for the switched page.
         */
        final boolean reuseLazy = isLazy() && initialized;
        debug(
                "[Pagination] Loading page %d (reuseLazy = %b, isStatic = %b, isComputed = %b, forceUpdated = %b)",
                currentPageIndex(), reuseLazy, isStatic(), isComputed(), forceUpdated);

        if ((isStatic() || reuseLazy) && !isComputed() && !forceUpdated) {
            // For unknown reasons already initialized but source is null, external modification?
            if (initialized && currSource == null)
                throw new IllegalStateException("User provided pagination source cannot be null");
            else {
                // Lazy pagination have pages count calculated on first render as a computed flow
                if (!isLazy()) pagesCount = calculatePagesCount(currSource);
            }

            final List<?> result =
                    Pagination.splitSourceForPage(currentPageIndex(), getPageSize(), getPagesCount(), currSource);
            debug(
                    "[Pagination] Split source of %d elements (page = %d, pageSize = %d, pagesCount = %d)",
                    result.size(), currentPageIndex(), getPageSize(), getPagesCount());
            int index = 0;
            for (final Object el : result) {
                debug("  | (%d): %s", index++, el);
            }

            return CompletableFuture.completedFuture(result);
        }

        isLoading = true;
        simulateStateUpdate();

        return createProvidedNewSource().handle((result, exception) -> {
            if (exception != null) {
                debug("[Pagination] An error occurred on data source computation: %s", exception.getMessage());
                exception.printStackTrace();
                return Collections.emptyList();
            }

            updateSource(result);
            isLoading = false;
            simulateStateUpdate();

            // Computed pagination always recomputes the full source (never reuses it, see the
            // condition above), so it must be sliced down to the current page just like lazy
            // pagination is, otherwise every page would render the same leading slice of items.
            if (isLazy() || isComputed())
                return Pagination.splitSourceForPage(currentPageIndex(), getPageSize(), getPagesCount(), result);
            else return result;
        });
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<List<?>> createProvidedNewSource() {
        CompletableFuture<List<?>> job = new CompletableFuture<>();

        final Object source = _srcFactory.apply(host);
        if (isAsync()) job = (CompletableFuture<List<?>>) source;
        else if (isComputed() || isLazy()) job.complete((List<?>) source);
        else throw new IllegalArgumentException("Unhandled pagination source");

        return job;
    }

    /**
     * Updates the current source and the number of available pages count based on that source.
     *
     * @param newSource The new data source.
     */
    private void updateSource(@NotNull List<?> newSource) {
        currSource = newSource;
        pagesCount = calculatePagesCount(currSource);
        debug("[Pagination] Source updated with %d elements and pages count set to %d", newSource.size(), pagesCount);
    }

    /**
     * The total number of pages available.
     *
     * @return The number of pages based on the current source.
     */
    private int getPagesCount() {
        return pagesCount;
    }

    /**
     * Calculates the number of pages available based on a given source.
     *
     * @param source The source to check.
     * @return The number of pages that can have based on the specified source.
     */
    private int calculatePagesCount(@NotNull List<?> source) {
        return (int) Math.ceil((double) source.size() / getPageSize());
    }

    /**
     * The current page size.
     *
     * @return Number of available elements position for pagination in the current page.
     */
    public int getPageSize() {
        if (pageSize == -1) throw new IllegalStateException("Page size need to be updated before try to get it");

        return pageSize;
    }

    /**
     * Loads pagination components using container boundaries, no constraints.
     * <p>
     * The position of the first paged item must be the first slot in the container, the last
     * position must be the last slot in the container, and {@link #pageSize} on the current page
     * must be the size of the container.
     *
     * @param context      The render context.
     * @param pageContents Elements of the current page.
     */
    private void addComponentsForUnconstrainedPagination(IFRenderContext context, List<?> pageContents) {
        final ViewContainer container = context.getContainer();

        // TODO Investigate why page size is being updated here
        if (pageSize == -1) updatePageSize(context);

        final int lastSlot = Math.min(container.getLastSlot() + 1 /* inclusive */, pageContents.size());
        for (int i = container.getFirstSlot(); i < lastSlot; i++) {
            final Object value = pageContents.get(i);
            final ComponentFactory factory = elementFactory.create(this, i, i, value);
            getInternalComponents().add(factory.create());
        }
    }

    /**
     * Renders the pagination using the layout positions defined from the {@link #getLayoutTarget() target layout character}.
     * <p>
     * The first position, last position and number of items on the page must be exactly the same as
     * the layout.
     *
     * @param context      The render context.
     * @param pageContents Elements of the current page.
     */
    private void addComponentsForLayeredPagination(IFRenderContext context, List<?> pageContents) {
        LayoutSlot layoutSlot = getLayoutSlotForCurrentTarget(context);
        debug("[Pagination] Is layout slot defined by the user? %b", layoutSlot.isDefinedByTheUser());

        final int contentSize = pageContents.size();
        debug("[Pagination] Elements count: %d elements", contentSize);
        debug("[Pagination] Iterating over '%c' layout target", layoutSlot.getCharacter());

        final int[] orderedPositions = computeSlotOrder(context, layoutSlot.getPositions());

        int index = 0;
        for (final int layoutPosition : orderedPositions) {
            if (index >= contentSize) {
                // A layout slot defined by the user mean the user probably want to add a placeholder
                // item to fill empty slots that couldn't be reached by the pagination. Happens when
                // using pagination and #layoutSlot with the same character as the pagination.
                if (layoutSlot.isDefinedByTheUser()) {
                    final ComponentFactory componentFactory =
                            layoutSlot.getFactory().apply(index);

                    if (componentFactory instanceof ItemComponentBuilder) {
                        final ItemComponentBuilder<?, ?> itemBuilder = (ItemComponentBuilder<?, ?>) componentFactory;

                        // In normal scenarios item position are set in LayoutRenderInterceptor
                        // but since it uses the same layout target as a Pagination component,
                        // This is the same behavior as `PaginationStateBuilder#elementFactory`.
                        itemBuilder.withSlot(layoutPosition);

                        // Marking as "externally managed" prevents other components from modifying
                        // this item accidentally.
                        itemBuilder.withExternallyManaged(true);
                    }

                    final Component component = componentFactory.create();

                    debug(() -> "  @ placeholder %d (index %d) = %s", layoutPosition, index, component.toString());
                    getInternalComponents().add(component);
                    index++;
                    continue;
                }

                break; // EOF
            }

            try {
                final Object paginatedValue = pageContents.get(index);
                final ComponentFactory factory = elementFactory.create(this, index, layoutPosition, paginatedValue);
                final Component component = factory.create();

                debug(() -> "  @ added %d (index %d) = %s", layoutPosition, index, component.toString());
                getInternalComponents().add(component);
            } catch (final Exception exception) {
                debug(() -> "  @ failed to add %d (index %d) = %s", layoutPosition, index, exception.getMessage());
                exception.printStackTrace();
            }

            index++;
        }
    }

    private int[] computeSlotOrder(IFRenderContext context, int[] positions) {
        Orientation orientation = getOrientation();
        String[] layout = context.getConfig().getLayout();

        final int rows = layout.length;
        final int cols = layout[0].length();
        final int totalSlots = rows * cols;

        boolean[] present = new boolean[totalSlots];
        for (int p : positions) {
            if (p >= 0 && p < totalSlots) present[p] = true;
        }

        List<Integer> rowMajor = new ArrayList<>(positions.length);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int slot = r * cols + c;
                if (present[slot]) rowMajor.add(slot);
            }
        }

        List<Integer> colMajor = new ArrayList<>(positions.length);
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                int slot = r * cols + c;
                if (present[slot]) colMajor.add(slot);
            }
        }

        if (orientation == Orientation.HORIZONTAL) {
            return listToIntArray(rowMajor);
        }
        if (orientation == Orientation.VERTICAL) {
            return listToIntArray(colMajor);
        }
        if (orientation == Orientation.ALTERNATING_ROWS) {
            return interleaveList(rowMajor);
        }
        if (orientation == Orientation.ALTERNATING_COLUMNS) {
            return interleaveList(colMajor);
        }

        if (orientation == Orientation.TOP_BOTTOM_LEFT_RIGHT) {
            boolean[] added = new boolean[totalSlots];
            List<Integer> out = new ArrayList<>(positions.length);

            for (int c = 0; c < cols; c++) {
                if (c == 0 || c == 2 || c == 4 || c == 6 || c == 8) {
                    // top -> bottom
                    for (int r = 0; r < rows; r++) {
                        int slot = r * cols + c;
                        if (slot >= 0 && slot < totalSlots && present[slot] && !added[slot]) {
                            out.add(slot);
                            added[slot] = true;
                        }
                    }
                } else {
                    // bottom -> top
                    for (int r = rows - 1; r >= 0; r--) {
                        int slot = r * cols + c;
                        if (slot >= 0 && slot < totalSlots && present[slot] && !added[slot]) {
                            out.add(slot);
                            added[slot] = true;
                        }
                    }
                }
            }

            if (out.size() < positions.length) {
                for (int rmSlot : rowMajor) {
                    if (!added[rmSlot]) {
                        out.add(rmSlot);
                        added[rmSlot] = true;
                    }
                }
            }

            return listToIntArray(out);
        }

        return positions;
    }

    private int[] listToIntArray(List<Integer> list) {
        int[] out = new int[list.size()];
        for (int i = 0; i < list.size(); i++) out[i] = list.get(i);
        return out;
    }

    private int[] interleaveList(List<Integer> list) {
        int n = list.size();
        int[] out = new int[n];
        int l = 0, r = n - 1, idx = 0;
        while (l <= r) {
            out[idx++] = list.get(l++);
            if (l <= r) out[idx++] = list.get(r--);
        }
        return out;
    }

    /**
     * Updates the current page size.
     * <p>
     * Page size is based on the type of pagination data source; on the possible usage of layout
     * in context, if a layout is configured in the layout so this property must be the count of
     * {@link #getLayoutTarget() layout target} characters in the layout configured layout.
     * <p>
     * When without a configured layout in the root, the page size is the entire size of context's container.
     *
     * @param context The render context.
     */
    private void updatePageSize(IFRenderContext context) {
        final boolean useLayout = context.getConfig().getLayout() != null;
        if (useLayout) pageSize = getLayoutSlotForCurrentTarget(context).getPositions().length;
        else pageSize = context.getContainer().getSize();

        debug(
                "[Pagination] Page size updated to %d (page = %d, useLayout = %b)",
                pageSize, currentPageIndex(), useLayout);
    }

    private LayoutSlot getLayoutSlotForCurrentTarget(IFRenderContext context) {
        if (currentLayoutSlot != null) return currentLayoutSlot;

        final Optional<LayoutSlot> layoutSlotOptional = context.getLayoutSlots().stream()
                .filter(layoutSlot -> layoutSlot.getCharacter() == getLayoutTarget())
                .max(Comparator.comparing(LayoutSlot::isDefinedByTheUser));

        if (!layoutSlotOptional.isPresent()) {
            // TODO more detailed error message
            throw new IllegalArgumentException(String.format("Layout slot target not found: %c", getLayoutTarget()));
        }

        return (currentLayoutSlot = layoutSlotOptional.get());
    }

    /**
     * Converts the user provided source provider to a valid static source.
     * <p>
     * Also, assigns the {@link #_srcFactory} value if the provided source has dynamic capabilities.
     *
     * @return The current source.
     * @throws IllegalArgumentException If the provided source is not supported.
     */
    @SuppressWarnings("unchecked")
    private List<?> convertSourceProvider() {
        if (sourceProvider instanceof Collection) {
            currSource = new ArrayList<>((Collection<?>) sourceProvider);
        } else if (sourceProvider instanceof Function) {
            _srcFactory = (Function<IFContext, Object>) sourceProvider;
        } else if (sourceProvider instanceof Supplier) {
            _srcFactory = $ -> ((Supplier<List<?>>) sourceProvider).get();
        } else {
            throw new IllegalArgumentException(String.format(
                    "Unsupported pagination source provider: %s",
                    sourceProvider.getClass().getName()));
        }

        return currSource;
    }

    /**
     * Loads the current page contents.
     *
     * @param context The render context.
     * @return A CompletableFuture with the completion stage of the current page.
     */
    private CompletableFuture<?> loadCurrentPage(IFRenderContext context) {
        return loadSourceForTheCurrentPage().thenAccept(pageContents -> {
            if (pageContents.isEmpty()) {
                debug("[Pagination] Empty page contents (page %d of %d)", currentPageIndex(), getPagesCount());
                return;
            }

            final boolean useLayout = context.getConfig().getLayout() != null;
            debug("[Pagination] Adding components.. (useLayout = %b)", useLayout);

            if (useLayout) addComponentsForLayeredPagination(context, pageContents);
            else addComponentsForUnconstrainedPagination(context, pageContents);
        });
    }

    public @NotNull IFContext getHost() {
        return host;
    }

    @Override
    public Object get() {
        return this;
    }

    @Override
    public void set(Object value) {
        // do nothing since Pagination is not immutable but unmodifiable directly
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public @NotNull VirtualView getRoot() {
        return host;
    }

    @Override
    public int getPosition() {
        final List<Component> components = getInternalComponents();
        if (components.isEmpty()) return 0;

        final int first = components.get(0).getPosition();
        final int last = components.get(components.size() - 1).getPosition();

        return last - first;
    }

    @Override
    public void render(@NotNull IFSlotRenderContext context) {
        if (!initialized) {
            setVisible(true);
            final IFRenderContext root = context.getParent();
            updatePageSize(root);
            loadCurrentPage(root).thenRun(() -> renderChild(context));
            initialized = true;
            return;
        }

        renderChild(context);
    }

    private void renderChild(IFSlotRenderContext context) {
        getInternalComponents().forEach(context::renderComponent);
    }

    @Override
    public void updated(@NotNull IFSlotRenderContext context) {
        final IFRenderContext renderContext = context.getParent();

        debug(
                "[Pagination] #updated(IFSlotRenderContext) called (forceUpdated = %b, pageWasChanged = %b)",
                forceUpdated, pageWasChanged);

        // Computed pagination must be recomputed on every update, not just on page change or a
        // forced update, since its whole premise is that the source is never reused/cached.
        // If page was changed all components will be removed, so don't trigger update on them
        if (forceUpdated || pageWasChanged || isComputed()) {
            clear(renderContext);
            components = new ArrayList<>();
            loadCurrentPage(renderContext).thenRun(() -> {
                render(context);
                simulateStateUpdate();
            });
            pageWasChanged = false;
            return;
        }

        if (!isVisible()) return;
        getInternalComponents().forEach(child -> child.updated(context));
    }

    /**
     * Simulate state update to call listeners thus calling watches in parent components.
     * <p>
     * Used when something changes in pagination. It allows the end user and developers to "listen"
     * for changes in {@link #isLoading()} and current page states.
     */
    private void simulateStateUpdate() {
        debug("[Pagination] State update simulation triggered on %d", getState().internalId());
        host.updateState(getState(), this);
    }

    @Override
    public void clear(@NotNull IFContext context) {
        debug("[Pagination] #clear(IFContext) called (pageWasChanged = %b)", pageWasChanged);
        if (!pageWasChanged) {
            getInternalComponents().forEach(child -> child.clear(context));
            return;
        }

        final Iterator<Component> childIterator = getInternalComponents().iterator();
        while (childIterator.hasNext()) {
            Component child = childIterator.next();
            child.clear(context);
            childIterator.remove();
        }
    }

    @Override
    public @UnmodifiableView Set<me.devnatan.inventoryframework.state.State<?>> getWatchingStates() {
        return Collections.emptySet();
    }

    @Override
    public @UnmodifiableView List<Component> getComponents() {
        return Collections.unmodifiableList(getInternalComponents());
    }

    @Override
    public List<Component> getInternalComponents() {
        return components;
    }

    @Override
    public boolean isContainedWithin(int position) {
        if (currentLayoutSlot != null) {
            for (int slot : currentLayoutSlot.getPositions()) {
                if (slot == position) return true;
            }
            return false;
        }

        for (final Component component : getInternalComponents()) {
            if (component.isContainedWithin(position)) return true;
        }
        return false;
    }

    @Override
    public boolean intersects(@NotNull Component other) {
        return Component.intersects(this, other);
    }

    @Override
    public InteractionHandler getInteractionHandler() {
        return this;
    }

    @Override
    public int currentPage() {
        return currentPageIndex() + 1;
    }

    @Override
    public int currentPageIndex() {
        return currPageIndex;
    }

    @Override
    public int nextPage() {
        return Math.min(getPagesCount(), currentPageIndex() + 1);
    }

    @Override
    public int nextPageIndex() {
        return Math.max(0, nextPage() - 1);
    }

    @Override
    public int lastPage() {
        return getPagesCount();
    }

    @Override
    public int lastPageIndex() {
        return Math.max(0, getPagesCount() - 1);
    }

    @Override
    public boolean isFirstPage() {
        return currentPageIndex() == 0;
    }

    @Override
    public boolean isLastPage() {
        return !canAdvance();
    }

    @Override
    public boolean hasPage(int pageIndex) {
        if (pageIndex < 0) return false;
        return pageIndex < getPagesCount();
    }

    @Override
    public void switchTo(int pageIndex) {
        debug("[Pagination] #switchTo(int) called (pageIndex = %d, isLoading = %b)", pageIndex, isLoading());
        if (!hasPage(pageIndex))
            throw new IndexOutOfBoundsException(
                    String.format("Page index not found (%d > %d)", pageIndex, getPagesCount()));

        if (isLoading()) return;

        currPageIndex = pageIndex;
        pageWasChanged = true;

        if (pageSwitchHandler != null) pageSwitchHandler.accept(host, this);

        host.updateComponent(this, false);
    }

    @Override
    public void advance() {
        if (!canAdvance()) return;
        switchTo(currentPageIndex() + 1);
    }

    @Override
    public boolean canAdvance() {
        return hasPage(currentPageIndex() + 1);
    }

    @Override
    public void back() {
        if (!canBack()) return;
        switchTo(currentPageIndex() - 1);
    }

    @Override
    public boolean canBack() {
        return hasPage(currentPageIndex() - 1);
    }

    @Override
    public char getLayoutTarget() {
        return layoutTarget;
    }

    @Override
    public boolean isLazy() {
        return isLazy;
    }

    @Override
    public boolean isStatic() {
        return isStatic;
    }

    @Override
    public boolean isComputed() {
        return isComputed;
    }

    @Override
    public boolean isAsync() {
        return isAsync;
    }

    @Override
    public boolean isLoading() {
        return isLoading;
    }

    @NotNull
    @Override
    public Iterator<Component> iterator() {
        return getComponents().iterator();
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        getInternalComponents().forEach(component -> component.setVisible(visible));
    }

    @Override
    public void clicked(@NotNull Component component, @NotNull IFSlotClickContext context) {
        // Lock child interactions while page is changing (specially for async pagination cases)
        if (pageWasChanged) {
            context.setCancelled(true);
            return;
        }

        for (final Component child : getInternalComponents()) {
            if (child.getInteractionHandler() == null || !child.isVisible()) continue;

            if (child.isContainedWithin(context.getClickedSlot())) {
                context.getParent()
                        .performClickInComponent(
                                child,
                                context.getViewer(),
                                context.getClickedContainer(),
                                context.getPlatformEvent(),
                                context.getClickedSlot(),
                                true);
                break;
            }
        }
    }

    @Override
    public boolean isManagedExternally() {
        return true;
    }

    @Override
    public boolean shouldRender(IFContext context) {
        return true;
    }

    @Override
    public void update() {
        ((IFContext) getRoot()).updateComponent(this, false);
    }

    @Override
    public void forceUpdate() {
        forceUpdated = true;
        update();
        forceUpdated = false;
    }

    @Override
    public void show() {
        setVisible(true);
        update();
    }

    @Override
    public void hide() {
        setVisible(false);
        update();
    }

    @Override
    public Ref<Component> getReference() {
        return null;
    }

    @Override
    public List<?> source() {
        return currSource;
    }

    @Override
    public Orientation getOrientation() {
        return orientation;
    }

    @Override
    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaginationImpl that = (PaginationImpl) o;
        return getLayoutTarget() == that.getLayoutTarget()
                && currPageIndex == that.currPageIndex
                && getPageSize() == that.getPageSize()
                && isLazy() == that.isLazy()
                && pageWasChanged == that.pageWasChanged
                && Objects.equals(sourceProvider, that.sourceProvider)
                && Objects.equals(pageSwitchHandler, that.pageSwitchHandler)
                && Objects.equals(orientation, that.orientation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getLayoutTarget(),
                sourceProvider,
                pageSwitchHandler,
                currPageIndex,
                getPageSize(),
                isLazy(),
                pageWasChanged,
                orientation);
    }

    @Override
    public String toString() {
        return "PaginationImpl{" + ", host="
                + host + ", layoutTarget="
                + layoutTarget + ", sourceProvider="
                + sourceProvider + ", elementFactory="
                + elementFactory + ", pageSwitchHandler="
                + pageSwitchHandler + ", currPageIndex="
                + currPageIndex + ", pageSize="
                + pageSize + ", dynamic="
                + isLazy + ", pageWasChanged="
                + pageWasChanged + ", _srcFactory="
                + _srcFactory + ", currSource="
                + currSource + ", orientation="
                + orientation + "} "
                + super.toString();
    }
}
