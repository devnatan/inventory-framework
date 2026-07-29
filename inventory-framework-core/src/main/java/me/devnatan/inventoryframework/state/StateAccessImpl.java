package me.devnatan.inventoryframework.state;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import me.devnatan.inventoryframework.component.ComponentFactory;
import me.devnatan.inventoryframework.component.ItemComponentBuilder;
import me.devnatan.inventoryframework.component.Pagination;
import me.devnatan.inventoryframework.component.PaginationElementFactory;
import me.devnatan.inventoryframework.component.PaginationImpl;
import me.devnatan.inventoryframework.component.PaginationStateBuilder;
import me.devnatan.inventoryframework.component.PaginationValueConsumer;
import me.devnatan.inventoryframework.context.IFContext;
import me.devnatan.inventoryframework.internal.ElementFactory;
import me.devnatan.inventoryframework.state.timer.TimerImpl;
import me.devnatan.inventoryframework.state.timer.TimerState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * <b><i> This is an internal inventory-framework API that should not be used from outside of
 * this library. No compatibility guarantees are provided. </i></b>
 */
@ApiStatus.Internal
public final class StateAccessImpl<
                Context extends IFContext,
                ItemBuilder extends ItemComponentBuilder<ItemBuilder, Context> & ComponentFactory>
        implements StateAccess<Context, ItemBuilder> {

    private final Object caller;
    private final Supplier<ElementFactory> elementFactoryProvider;
    private final StateRegistry stateRegistry;

    public StateAccessImpl(
            Object caller, Supplier<ElementFactory> elementFactoryProvider, StateRegistry stateRegistry) {
        this.caller = caller;
        this.elementFactoryProvider = elementFactoryProvider;
        this.stateRegistry = stateRegistry;
    }

    @Override
    public final <T> State<T> state(T initialValue) {
        final long id = State.next();
        final StateValueFactory factory = (host, state) -> new ImmutableValue(state, initialValue);
        final State<T> state = new BaseState<>(id, factory);
        stateRegistry.registerState(state, this);

        return state;
    }

    @Override
    public final <T> MutableState<T> mutableState(T initialValue) {
        final long id = State.next();
        final StateValueFactory factory = (host, state) -> new MutableValue(state, initialValue);
        final MutableState<T> state = new MutableGenericStateImpl<>(id, factory);
        this.stateRegistry.registerState(state, this);

        return state;
    }

    @Override
    public final MutableIntState mutableState(int initialValue) {
        final long id = State.next();
        final StateValueFactory factory = (host, state) -> new MutableValue(state, initialValue);
        final MutableIntState state = new MutableIntStateImpl(id, factory);
        this.stateRegistry.registerState(state, this);

        return state;
    }

    @Override
    public final <T> State<T> computedState(@NotNull Function<Context, T> computation) {
        final long id = State.next();
        @SuppressWarnings("unchecked")
        final StateValueFactory factory =
                (host, state) -> new ComputedValue(state, () -> computation.apply((Context) host));
        final State<T> state = new BaseState<>(id, factory);
        this.stateRegistry.registerState(state, this);

        return state;
    }

    @Override
    public final <T> State<T> computedState(@NotNull Supplier<T> computation) {
        final long id = State.next();
        final StateValueFactory factory = (host, state) -> new ComputedValue(state, computation);
        final State<T> state = new BaseState<>(id, factory);
        this.stateRegistry.registerState(state, this);

        return state;
    }

    @Override
    public final <T> State<T> lazyState(@NotNull Function<Context, T> computation) {
        final long id = State.next();
        @SuppressWarnings("unchecked")
        final StateValueFactory factory =
                (host, state) -> new LazyValue(state, () -> computation.apply((Context) host));
        final State<T> state = new BaseState<>(id, factory);
        this.stateRegistry.registerState(state, this);

        return state;
    }

    @Override
    public final <T> State<T> lazyState(@NotNull Supplier<T> computation) {
        final long id = State.next();
        final StateValueFactory factory = (host, state) -> new LazyValue(state, computation);
        final State<T> state = new BaseState<>(id, factory);
        this.stateRegistry.registerState(state, this);

        return state;
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public final <T> MutableState<T> initialState() {
        return initialState(null);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public final <T> MutableState<T> initialState(@NotNull String key) {
        final long id = State.next();
        final MutableState<T> state =
                new BaseMutableState<>(id, (host, valueState) -> new InitialDataStateValue(valueState, host, key));
        this.stateRegistry.registerState(state, this);

        return state;
    }

    @Override
    public TimerState timerState(long intervalInTicks) {
        final long id = State.next();
        final StateValueFactory factory =
                (host, state) -> new ImmutableValue(state, new TimerImpl(intervalInTicks, ((IFContext) host)::update));
        final TimerState state = new TimerStateImpl(id, factory);
        this.stateRegistry.registerState(state, this);

        return state;
    }

    @Override
    public final <T> State<Pagination> paginationState(
            @NotNull List<? super T> sourceProvider,
            @NotNull PaginationValueConsumer<Context, ItemBuilder, T> elementConsumer) {
        return this.<T>buildPaginationState(sourceProvider)
                .elementFactory(elementConsumer)
                .build();
    }

    @Override
    public final <T> State<Pagination> computedPaginationState(
            @NotNull Function<Context, List<? super T>> sourceProvider,
            @NotNull PaginationValueConsumer<Context, ItemBuilder, T> valueConsumer) {
        return this.buildComputedPaginationState(sourceProvider)
                .elementFactory(valueConsumer)
                .build();
    }

    @Override
    public final <T> State<Pagination> computedAsyncPaginationState(
            @NotNull Function<Context, CompletableFuture<List<T>>> sourceProvider,
            @NotNull PaginationValueConsumer<Context, ItemBuilder, T> valueConsumer) {
        return this.buildComputedAsyncPaginationState(sourceProvider)
                .elementFactory(valueConsumer)
                .build();
    }

    @Override
    public final <T> State<Pagination> lazyPaginationState(
            @NotNull Function<Context, List<? super T>> sourceProvider,
            @NotNull PaginationValueConsumer<Context, ItemBuilder, T> valueConsumer) {
        return this.buildLazyPaginationState(sourceProvider)
                .elementFactory(valueConsumer)
                .build();
    }

    @Override
    public <T> State<Pagination> lazyPaginationState(
            @NotNull Supplier<List<? super T>> sourceProvider,
            @NotNull PaginationValueConsumer<Context, ItemBuilder, T> valueConsumer) {
        return this.buildLazyPaginationState(sourceProvider)
                .elementFactory(valueConsumer)
                .build();
    }

    @Override
    public <T> State<Pagination> lazyAsyncPaginationState(
            @NotNull Function<Context, CompletableFuture<List<T>>> sourceProvider,
            @NotNull PaginationValueConsumer<Context, ItemBuilder, T> valueConsumer) {
        return this.buildLazyAsyncPaginationState(sourceProvider)
                .elementFactory(valueConsumer)
                .build();
    }

    @Override
    public final <T> PaginationStateBuilder<Context, ItemBuilder, T> buildPaginationState(
            @NotNull List<? super T> sourceProvider) {
        return new PaginationStateBuilder<>(
                elementFactoryProvider, sourceProvider, this::createPaginationState, false, false);
    }

    @Override
    public final <T> PaginationStateBuilder<Context, ItemBuilder, T> buildComputedPaginationState(
            @NotNull Function<Context, List<? super T>> sourceProvider) {
        return new PaginationStateBuilder<>(
                elementFactoryProvider, sourceProvider, this::createPaginationState, false, true);
    }

    @Override
    public final <T> PaginationStateBuilder<Context, ItemBuilder, T> buildComputedAsyncPaginationState(
            @NotNull Function<Context, CompletableFuture<List<T>>> sourceProvider) {
        return new PaginationStateBuilder<>(
                elementFactoryProvider, sourceProvider, this::createPaginationState, true, true);
    }

    @Override
    public final <T> PaginationStateBuilder<Context, ItemBuilder, T> buildLazyPaginationState(
            @NotNull Supplier<List<? super T>> sourceProvider) {
        return new PaginationStateBuilder<>(
                elementFactoryProvider, sourceProvider, this::createPaginationState, false, false);
    }

    @Override
    public final <T> PaginationStateBuilder<Context, ItemBuilder, T> buildLazyPaginationState(
            @NotNull Function<Context, List<? super T>> sourceProvider) {
        return new PaginationStateBuilder<>(
                elementFactoryProvider, sourceProvider, this::createPaginationState, false, false);
    }

    @Override
    public final <T> PaginationStateBuilder<Context, ItemBuilder, T> buildLazyAsyncPaginationState(
            @NotNull Function<Context, CompletableFuture<List<T>>> sourceProvider) {
        return new PaginationStateBuilder<>(
                elementFactoryProvider, sourceProvider, this::createPaginationState, true, false);
    }

    protected final <V> State<Pagination> createPaginationState(
            @NotNull PaginationStateBuilder<Context, ItemBuilder, V> builder) {
        final long id = State.next();
        @SuppressWarnings({"unchecked", "rawtypes"})
        final StateValueFactory factory = (host, state) -> new PaginationImpl(
                state,
                (IFContext) host,
                builder.getLayoutTarget(),
                builder.getSourceProvider(),
                (PaginationElementFactory) builder.getPaginationElementFactory(),
                (BiConsumer) builder.getPageSwitchHandler(),
                builder.isAsync(),
                builder.isComputed(),
                builder.getOrientation());
        final State<Pagination> state = new PaginationState(id, factory);
        this.stateRegistry.registerState(state, caller);

        return state;
    }
}
