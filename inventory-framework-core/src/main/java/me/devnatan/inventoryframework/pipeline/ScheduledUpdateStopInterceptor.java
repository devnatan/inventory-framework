package me.devnatan.inventoryframework.pipeline;

import me.devnatan.inventoryframework.RootView;
import me.devnatan.inventoryframework.VirtualView;
import me.devnatan.inventoryframework.context.IFCloseContext;
import me.devnatan.inventoryframework.context.IFContext;

public final class ScheduledUpdateStopInterceptor implements PipelineInterceptor<VirtualView> {

    @Override
    public void intercept(PipelineContext<VirtualView> pipeline, VirtualView subject) {
        if (pipeline.getPhase() != StandardPipelinePhases.VIEWER_REMOVED) return;

        final IFCloseContext context = (IFCloseContext) subject;
        final long updateIntervalInTicks = context.getConfig().getUpdateIntervalInTicks();
        if (updateIntervalInTicks == 0 && context.getConfig().getUpdateIntervalState() == null) {
            return;
        }

        final RootView root = context.getRoot();
        if (root.getScheduledUpdateJob() == null
                || !root.getScheduledUpdateJob().isStarted()) {
            return;
        }

        if (context.isCancelled() || !context.isActive()) {
            return;
        }

        if (root.getInternalContexts().stream().filter(IFContext::isActive).count() > 1) {
            return;
        }

        root.getScheduledUpdateJob().cancel();
    }
}
