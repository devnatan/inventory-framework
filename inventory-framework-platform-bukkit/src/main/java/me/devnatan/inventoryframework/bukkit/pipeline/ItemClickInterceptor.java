package me.devnatan.inventoryframework.bukkit.pipeline;

import me.devnatan.inventoryframework.VirtualView;
import me.devnatan.inventoryframework.bukkit.context.SlotClickContext;
import me.devnatan.inventoryframework.component.Component;
import me.devnatan.inventoryframework.component.ItemComponent;
import me.devnatan.inventoryframework.pipeline.PipelineContext;
import me.devnatan.inventoryframework.pipeline.PipelineInterceptor;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.jetbrains.annotations.NotNull;

/**
 * Intercepted when a player clicks on an item the view container.
 */
public final class ItemClickInterceptor implements PipelineInterceptor<VirtualView> {

    @Override
    public void intercept(@NotNull PipelineContext<VirtualView> pipeline, @NotNull VirtualView subject) {
        if (!(subject instanceof SlotClickContext)) return;

        final SlotClickContext context = (SlotClickContext) subject;
        final InventoryClickEvent event = context.getClickOrigin();
        if (event.getSlotType() == InventoryType.SlotType.OUTSIDE) return;

        final Component component = context.getComponent();
        if (component == null) return;

        if (component instanceof ItemComponent) {
            final ItemComponent item = (ItemComponent) component;

            // inherit cancellation so we can un-cancel it
            context.setCancelled(item.isCancelOnClick());
        }
    }
}
