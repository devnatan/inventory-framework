package me.devnatan.inventoryframework.state;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import me.devnatan.inventoryframework.component.ComponentFactory;
import me.devnatan.inventoryframework.component.ItemComponentBuilder;
import me.devnatan.inventoryframework.component.Pagination;
import me.devnatan.inventoryframework.component.PaginationStateBuilder;
import me.devnatan.inventoryframework.component.PaginationValueConsumer;
import me.devnatan.inventoryframework.context.IFContext;
import me.devnatan.inventoryframework.context.IFOpenContext;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public interface StateAccess<
        Context extends IFContext, ItemBuilder extends ItemComponentBuilder<ItemBuilder, Context> & ComponentFactory> {

    /**
     * Creates an immutable state with an initial value.
     *
     * <pre>{@code
     * State<String> textState = state("test");
     *
     * intState.get(...); // "test"
     * }</pre>
     *
     * @param initialValue The initial value of the state.
     * @param <T>          The state value type.
     * @return A state with an initial value.
     */
    <T> State<T> state(T initialValue);

    /**
     * Creates a {@link MutableState mutable state} with an initial value.
     *
     * <pre>{@code
     * MutableState<String> textState = mutableState("");
     *
     * textState.get(...); // ""
     * textState.set("abc", ...);
     * textState.get(...); // "abc"
     * }</pre>
     *
     * @param initialValue The initial value of the state.
     * @param <T>          The state value type.
     * @return A mutable state with an initial value.
     */
    <T> MutableState<T> mutableState(T initialValue);

    /**
     * Creates a {@link MutableState mutable state} with an initial value.
     *
     * <pre>{@code
     * MutableIntState intState = mutableIntState(0);
     *
     * intState.get(...); // 0
     * intState.set(4, ...);
     * intState.get(...); // 4
     * }</pre>
     *
     * @param initialValue The initial value of the state.
     * @return A mutable state with an initial value.
     */
    MutableIntState mutableState(int initialValue);

    /**
     * Creates an immutable computed state.
     * <p>
     * A computed state is a state that every time an attempt is made to obtain the value of that
     * state, the obtained value is computed again by the {@code computation} function.
     * <pre>{@code
     * State<Integer> intState = computedState($ -> ThreadLocalRandom.current().nextInt());
     *
     * intState.get(...); // some random number
     * intState.get(...); // another random number
     * }</pre>
     *
     * @param computation The function to compute the value.
     * @param <T>         The state value type.
     * @return An immutable computed state.
     */
    <T> State<T> computedState(@NotNull Function<Context, T> computation);

    /**
     * Creates an immutable computed state.
     * <p>
     * A computed state is a state that every time an attempt is made to obtain the value of that
     * state, the obtained value is computed again by the {@code computation} function.
     * <pre>{@code
     * State<Integer> randomIntState = computedState(ThreadLocalRandom.current()::nextInt);
     *
     * randomIntState.get(...); // some random number
     * randomIntState.get(...); // another random number
     * }</pre>
     *
     * @param computation The function to compute the value.
     * @param <T>         The state holder type.
     * @return An immutable computed state.
     */
    <T> State<T> computedState(@NotNull Supplier<T> computation);

    /**
     * Creates an immutable lazy state.
     * <p>
     * {@code factory} defines what the value will be, a holder try to get the value, and the value
     * obtained from there will be the value that will be obtained in subsequent calls to get the
     * value of the state.
     * <pre>{@code
     * State<Integer> intState = lazyState($ -> ThreadLocalRandom.current().nextInt());
     *
     * intState.get(...); // 54 - from initial computation of random integer ^^
     * intState.get(...); // 54 - previously defined by the initial computation
     * }</pre>
     *
     * @param computation The value factory.
     * @param <T>         The state value type.
     * @return A lazy state.
     */
    <T> State<T> lazyState(@NotNull Function<Context, T> computation);

    /**
     * Creates an immutable lazy state.
     * <p>
     * {@code factory} defines what the value will be, a holder try to get the value, and the value
     * obtained from there will be the value that will be obtained in subsequent calls to get the
     * value of the state.
     * <pre>{@code
     * State<Integer> intState = lazyState(ThreadLocalRandom.current()::nextInt);
     *
     * intState.get(...); // 54 - from initial computation of random integer ^^
     * intState.get(...); // 54 - previously defined by the initial computation
     * }</pre>
     *
     * @param computation The value factory.
     * @param <T>         The state holder type.
     * @return A lazy state.
     */
    <T> State<T> lazyState(@NotNull Supplier<T> computation);

    /**
     * Creates an immutable lazy asynchronous state.
     * <p>
     * This method expects you to return a {@link CompletableFuture} in <code>computation</code>
     * parameter so elements in the view that are watching that state can hook to it and wait
     * until that future gets completed to proceed with whatever they are willing to do.
     * <p>
     * <b>This API is experimental and is not subject to the general compatibility guarantees such API
     * may be changed or may be removed completely in any further release.</b>
     *
     * @param computation The value factory.
     * @param <T>         The state value type.
     * @return A new lazy asynchronous state instance.
     */
    @ApiStatus.Experimental
    <T> State<T> lazyAsyncState(@NotNull Supplier<CompletableFuture<T>> computation);

    /**
     * Creates an immutable lazy asynchronous state.
     * <p>
     * This method expects you to return a {@link CompletableFuture} in <code>computation</code>
     * parameter so elements in the view that are watching that state can hook to it and wait
     * until that future gets completed to proceed with whatever they are willing to do.
     * <p>
     * <b>This API is experimental and is not subject to the general compatibility guarantees such API
     * may be changed or may be removed completely in any further release.</b>
     *
     * @param computation The value factory.
     * @param <T>         The state value type.
     * @return A new lazy asynchronous state instance.
     */
    @ApiStatus.Experimental
    <T> State<T> lazyAsyncState(@NotNull Function<Context, CompletableFuture<T>> computation);

    /**
     * Creates a mutable {@link #lazyState(Function) lazy state} whose value is always computed
     * from the initial data set by its {@link StateValueHost}.
     * <p>
     * When the holder is a {@code OpenViewContext}, the initial value will be the value defined
     * in the initial opening data of the container. This state is specifically set for backwards
     * compatibility with the old way of applying data to a context before or during container open.
     * <p>
     * The class parameter is used to convert all initial state into a value. Note that support for
     * obtaining a specific value from the initial data is only available from version 2.5.4 of the
     * library.
     *
     * @param <T> The initial data type.
     * @return A state computed with an initial opening data value.
     */
    <T> MutableState<T> initialState();

    /**
     * Creates a mutable {@link #lazyState(Function) lazy state} whose value is always computed
     * from the initial data set by its {@link StateValueHost}.
     * <p>
     * When the holder is a {@link IFOpenContext}, the initial value will be the value defined
     * in the initial opening data of the container. This state is specifically set for backwards
     * compatibility with the old way of applying data to a context before or during container open.
     * <p>
     * As to open a view it is necessary to pass a {@link java.util.Map}, the {@code key} is used to
     * get the value from that map.
     *
     * @param key The initial data identifier.
     * @param <T> The initial data value type.
     * @return A state computed with an initial opening data value.
     */
    <T> MutableState<T> initialState(@NotNull String key);

    /**
     * Creates a new immutable pagination with static data source.
     *
     * @param sourceProvider The data source for pagination.
     * @param elementConsumer The function for creating pagination items, this function is called for
     *                       each paged element (item) on a page.
     * @param <T>            The pagination data type.
     * @return A new immutable pagination state.
     */
    <T> State<Pagination> paginationState(
            @NotNull List<? super T> sourceProvider,
            @NotNull PaginationValueConsumer<Context, ItemBuilder, T> elementConsumer);

    /**
     * Creates a new unmodifiable computed pagination state.
     *
     * @param sourceProvider Data source for pagination.
     * @param valueConsumer  Function for creating pagination items, this function is called for
     *                       each paged element (item) on a page.
     * @param <T>            The pagination data type.
     * @return A new unmodifiable pagination state.
     */
    <T> State<Pagination> computedPaginationState(
            @NotNull Function<Context, List<? super T>> sourceProvider,
            @NotNull PaginationValueConsumer<Context, ItemBuilder, T> valueConsumer);

    /**
     * Creates a new unmodifiable computed pagination state with asynchronous data source.
     * <p>
     * <b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     *
     * @param sourceProvider The data source for pagination.
     * @param valueConsumer   The function for creating pagination items, this function is called for
     *                       each paged element (item) on a page.
     * @param <T>            The pagination data type.
     * @return A new unmodifiable pagination state.
     */
    @ApiStatus.Experimental
    <T> State<Pagination> computedAsyncPaginationState(
            @NotNull Function<Context, CompletableFuture<List<T>>> sourceProvider,
            @NotNull PaginationValueConsumer<Context, ItemBuilder, T> valueConsumer);

    /**
     * Creates a new unmodifiable lazy pagination state.
     *
     * @param sourceProvider Data source for pagination.
     * @param valueConsumer  Function for creating pagination items, this function is called for
     *                       each paged element (item) on a page.
     * @param <T>            The pagination data type.
     * @return A new unmodifiable pagination state.
     */
    <T> State<Pagination> lazyPaginationState(
            @NotNull Function<Context, List<? super T>> sourceProvider,
            @NotNull PaginationValueConsumer<Context, ItemBuilder, T> valueConsumer);

    /**
     * Creates a new unmodifiable lazy pagination state.
     *
     * @param sourceProvider Data source for pagination.
     * @param valueConsumer  Function for creating pagination items, this function is called for
     *                       each paged element (item) on a page.
     * @param <T>            The pagination data type.
     * @return A new unmodifiable pagination state.
     */
    <T> State<Pagination> lazyPaginationState(
            @NotNull Supplier<List<? super T>> sourceProvider,
            @NotNull PaginationValueConsumer<Context, ItemBuilder, T> valueConsumer);

    /**
     * Creates a new unmodifiable lazy pagination state with asynchronous data source.
     * <p>
     * <b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     *
     * @param sourceProvider The data source for pagination.
     * @param valueConsumer    The function for creating pagination items, this function is called for
     *                       each paged element (item) on a page.
     * @param <T>            The pagination data type.
     * @return A new unmodifiable pagination state.
     */
    @ApiStatus.Experimental
    <T> State<Pagination> lazyAsyncPaginationState(
            @NotNull Function<Context, CompletableFuture<List<T>>> sourceProvider,
            @NotNull PaginationValueConsumer<Context, ItemBuilder, T> valueConsumer);

    /**
     * Creates a new unmodifiable static pagination state builder.
     *
     * @param sourceProvider The data source for pagination.
     * @param <T>            The pagination data type.
     * @return A new pagination state builder.
     */
    <T> PaginationStateBuilder<Context, ItemBuilder, T> buildPaginationState(@NotNull List<? super T> sourceProvider);

    /**
     * Creates a new unmodifiable dynamic pagination state builder.
     *
     * @param sourceProvider The data source for pagination.
     * @param <T>            The pagination data type.
     * @return A new pagination state builder.
     */
    <T> PaginationStateBuilder<Context, ItemBuilder, T> buildComputedPaginationState(
            @NotNull Function<Context, List<? super T>> sourceProvider);

    /**
     * Creates a new unmodifiable computed pagination state builder with asynchronous data source.
     * <p>
     * <b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     *
     * @param sourceProvider The data source for pagination.
     * @param <T>            The pagination data type.
     * @return A new pagination state builder.
     */
    @ApiStatus.Experimental
    <T> PaginationStateBuilder<Context, ItemBuilder, T> buildComputedAsyncPaginationState(
            @NotNull Function<Context, CompletableFuture<List<T>>> sourceProvider);

    /**
     * Creates a new unmodifiable lazy pagination state builder.
     *
     * @param sourceProvider The data source for pagination.
     * @param <T>            The pagination data type.
     * @return A new pagination state builder.
     */
    <T> PaginationStateBuilder<Context, ItemBuilder, T> buildLazyPaginationState(
            @NotNull Supplier<List<? super T>> sourceProvider);

    /**
     * Creates a new unmodifiable lazy pagination state builder.
     *
     * @param sourceProvider The data source for pagination.
     * @param <T>            The pagination data type.
     * @return A new pagination state builder.
     */
    <T> PaginationStateBuilder<Context, ItemBuilder, T> buildLazyPaginationState(
            @NotNull Function<Context, List<? super T>> sourceProvider);

    /**
     * Creates a new unmodifiable lazy pagination state builder with asynchronous data source.
     * <p>
     * <b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     *
     * @param sourceProvider The data source for pagination.
     * @param <T>            The pagination data type.
     * @return A new pagination state builder.
     */
    @ApiStatus.Experimental
    <T> PaginationStateBuilder<Context, ItemBuilder, T> buildLazyAsyncPaginationState(
            @NotNull Function<Context, CompletableFuture<List<T>>> sourceProvider);
}
