package me.devnatan.inventoryframework.state.timer;

import me.devnatan.inventoryframework.state.State;
import org.jetbrains.annotations.ApiStatus;

/**
 * A state that holds a {@link Timer}.
 *
 * <p><b><i> This API is experimental and is not subject to the general compatibility guarantees
 * such API may be changed or may be removed completely in any further release. </i></b>
 */
@ApiStatus.Experimental
public interface TimerState extends State<Timer> {}
