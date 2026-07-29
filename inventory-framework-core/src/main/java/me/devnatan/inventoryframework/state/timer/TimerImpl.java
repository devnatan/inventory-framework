package me.devnatan.inventoryframework.state.timer;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Default {@link Timer} implementation.
 * <p>
 * A {@link #loop()} call is expected to happen every base tick, this timer tracks its own
 * elapsed ticks and fires {@code onElapsed} once {@link #currentInterval()} ticks have passed,
 * so that changing the interval or pausing takes effect immediately without needing to
 * reschedule the underlying platform job.
 *
 * <p><b><i> This is an internal inventory-framework API that should not be used from outside of
 * this library. No compatibility guarantees are provided. </i></b>
 */
@ApiStatus.Internal
public final class TimerImpl implements Timer {

    private final long initialInterval;
    private final Runnable onElapsed;
    private long currentInterval;
    private long elapsedTicks;
    private boolean paused;

    public TimerImpl(long initialInterval, @NotNull Runnable onElapsed) {
        this.initialInterval = initialInterval;
        this.currentInterval = initialInterval;
        this.onElapsed = onElapsed;
    }

    @Override
    public void loop() {
        if (paused || currentInterval <= 0) return;

        elapsedTicks++;
        if (elapsedTicks < currentInterval) return;

        elapsedTicks = 0;
        onElapsed.run();
    }

    @Override
    public long initialInterval() {
        return initialInterval;
    }

    @Override
    public long currentInterval() {
        return currentInterval;
    }

    @Override
    public void changeInterval(long interval) {
        this.currentInterval = interval;
        this.elapsedTicks = 0;
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    @Override
    public boolean pause() {
        paused = !paused;
        return paused;
    }
}
