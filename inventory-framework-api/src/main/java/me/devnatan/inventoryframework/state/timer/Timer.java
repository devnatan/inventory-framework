package me.devnatan.inventoryframework.state.timer;

import org.jetbrains.annotations.ApiStatus;

/**
 * A timer whose interval and pause state can be controlled at runtime.
 * <p>
 * Unlike {@link me.devnatan.inventoryframework.ViewConfigBuilder#scheduleUpdate(long)}, a timer
 * lets the developer pause it, resume it and change its interval without having to open a new
 * view.
 *
 * <p><b><i> This API is experimental and is not subject to the general compatibility guarantees
 * such API may be changed or may be removed completely in any further release. </i></b>
 */
@ApiStatus.Experimental
public interface Timer {

    /**
     * Called every base tick by the underlying scheduled job.
     * <p>
     * This is an internal detail of the timer implementation, it should not be called directly.
     */
    @ApiStatus.Internal
    void loop();

    /**
     * The interval, in ticks, that this timer was created with.
     *
     * @return The initial interval in ticks.
     */
    long initialInterval();

    /**
     * The interval, in ticks, that this timer is currently running at.
     *
     * @return The current interval in ticks.
     */
    long currentInterval();

    /**
     * Changes the interval of this timer.
     * <p>
     * Takes effect immediately, the timer's elapsed time is reset.
     *
     * @param interval The new interval in ticks.
     */
    void changeInterval(long interval);

    /**
     * Whether this timer is currently paused.
     *
     * @return {@code true} if paused, {@code false} otherwise.
     */
    boolean isPaused();

    /**
     * Toggles the paused state of this timer.
     *
     * @return The new paused state, i.e. {@code true} if the timer is now paused.
     */
    boolean pause();
}
