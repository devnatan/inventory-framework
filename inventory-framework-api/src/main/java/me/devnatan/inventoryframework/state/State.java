package me.devnatan.inventoryframework.state;

import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * States are just intermediate interfaces to a {@link StateValueHost} and a {@link StateValue}.
 * <p>
 * A state has a unique id that's passed on to any value defined from it, a state does not keep any
 * data other than its id, who keeps it is the state value. A state id is needed to access a
 * state value within a value host.
 *
 * @param <T> The state value type.
 * @see MutableState
 */
public interface State<T> {

    /**
     * <p><b><i> This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided. </i></b>
     */
    @ApiStatus.Internal
    AtomicLong ids = new AtomicLong();

    /**
     * Gets the current value for this state defined in the specified host.
     *
     * @param host The state host.
     * @return The current state value.
     */
    T get(@NotNull StateValueHost host);

    /**
     * The value factory of this state.
     *
     * <p><b><i> This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided. </i></b>
     *
     * @return The internal value factory for this state.
     */
    @ApiStatus.Internal
    StateValueFactory factory();

    /**
     * Returns the internal unique id of this state.
     *
     * <p><b><i> This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided. </i></b>
     *
     * @return The state id.
     */
    @ApiStatus.Internal
    long internalId();

    /**
     * Generates a new state id.
     *
     * <p><b><i> This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided. </i></b>
     *
     * @return A new unique state id.
     */
    @ApiStatus.Internal
    static long next() {
        return ids.getAndIncrement();
    }
}
