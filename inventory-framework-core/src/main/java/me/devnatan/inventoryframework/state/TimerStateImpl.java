package me.devnatan.inventoryframework.state;

import me.devnatan.inventoryframework.state.timer.Timer;
import me.devnatan.inventoryframework.state.timer.TimerState;
import org.jetbrains.annotations.ApiStatus;

/**
 * <b><i> This is an internal inventory-framework API that should not be used from outside of
 * this library. No compatibility guarantees are provided. </i></b>
 */
@ApiStatus.Internal
public final class TimerStateImpl extends BaseState<Timer> implements TimerState {

    public TimerStateImpl(long id, StateValueFactory valueFactory) {
        super(id, valueFactory);
    }
}
