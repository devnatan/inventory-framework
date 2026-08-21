package me.devnatan.inventoryframework.pipeline;

import static me.devnatan.inventoryframework.ViewConfig.CANCEL_ON_CLICK;

import me.devnatan.inventoryframework.VirtualView;
import me.devnatan.inventoryframework.context.SlotClickContext;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Intercepted when a player clicks on the view container.
 * If the click is canceled, this interceptor ends the pipeline immediately.
 */
public final class GlobalClickInterceptor implements PipelineInterceptor<VirtualView> {

    @Override
    public void intercept(@NotNull PipelineContext<VirtualView> pipeline, @NotNull VirtualView subject) {
        if (!(subject instanceof SlotClickContext)) return;

        final SlotClickContext context = (SlotClickContext) subject;
        final InventoryClickEvent event = context.getClickOrigin();

        final boolean allowedOnEntityContainer =
                context.isOnEntityContainer() && context.getConfig().isEntityContainerInteractionsAllowed();

        // inherit cancellation so we can un-cancel it
        context.setCancelled(event.isCancelled()
                || (context.getConfig().isOptionSet(CANCEL_ON_CLICK, true) && !allowedOnEntityContainer));
        context.getRoot().onClick(context);
    }
}
