package me.devnatan.inventoryframework.pipeline;

import java.util.ArrayList;
import java.util.List;
import me.devnatan.inventoryframework.RootView;
import me.devnatan.inventoryframework.VirtualView;
import me.devnatan.inventoryframework.context.IFContext;
import me.devnatan.inventoryframework.internal.Job;
import me.devnatan.inventoryframework.state.timer.Timer;
import me.devnatan.inventoryframework.state.timer.TimerState;

public final class ScheduledUpdateStartInterceptor implements PipelineInterceptor<VirtualView> {

    /** Base cadence, in ticks, the timer-driven job runs at so a {@link Timer}'s own interval can change at runtime. */
    private static final long TIMER_BASE_INTERVAL_IN_TICKS = 1;

    @Override
    public void intercept(PipelineContext<VirtualView> pipeline, VirtualView subject) {
        if (pipeline.getPhase() != StandardPipelinePhases.VIEWER_ADDED) return;

        final IFContext context = (IFContext) subject;
        final RootView root = context.getRoot();
        final long updateIntervalInTicks = context.getConfig().getUpdateIntervalInTicks();
        final TimerState updateIntervalState = context.getConfig().getUpdateIntervalState();

        if (updateIntervalInTicks == 0 && updateIntervalState == null) {
            return;
        }

        if (root.getScheduledUpdateJob() != null && root.getScheduledUpdateJob().isStarted()) {
            return;
        }

        final Job updateJob;
        if (updateIntervalState != null) {
            updateJob = root.getElementFactory().scheduleJobInterval(root, TIMER_BASE_INTERVAL_IN_TICKS, () -> {
                final List<IFContext> contextList = new ArrayList<>(root.getInternalContexts());
                contextList.stream()
                        .filter(IFContext::isActive)
                        .forEach(activeContext -> ((Timer) updateIntervalState.get(activeContext)).loop());
            });
        } else {
            updateJob = root.getElementFactory().scheduleJobInterval(root, updateIntervalInTicks, () -> {
                final List<IFContext> contextList = new ArrayList<>(root.getInternalContexts());
                contextList.stream().filter(IFContext::isActive).forEach(IFContext::update);
            });
        }
        root.setScheduledUpdateJob(updateJob);
        updateJob.start();
    }
}
