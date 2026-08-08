package me.devnatan.inventoryframework;

import me.devnatan.inventoryframework.component.Component;
import me.devnatan.inventoryframework.context.IFCloseContext;
import me.devnatan.inventoryframework.context.IFContext;
import me.devnatan.inventoryframework.context.IFRenderContext;
import me.devnatan.inventoryframework.context.IFSlotClickContext;
import me.devnatan.inventoryframework.pipeline.StandardPipelinePhases;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;

final class IFInventoryListener implements Listener {

    private final ViewFrame viewFrame;

    public IFInventoryListener(ViewFrame viewFrame) {
        this.viewFrame = viewFrame;
    }

    @EventHandler
    public void onPluginDisable(final PluginDisableEvent event) {
        if (!event.getPlugin().getName().equals(viewFrame.getOwner().getName())) return;

        viewFrame.unregister();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.getPlayer().closeInventory(); // todo add a new way to invalidate views properly
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        final Player player = (Player) event.getWhoClicked();
        try {
            final Viewer viewer = viewFrame.getViewer(player);
            if (viewer == null) return;

            final IFRenderContext context = viewer.getActiveContext();
            final Component clickedComponent = context.getComponentsAt(event.getRawSlot()).stream()
                    .filter(Component::isVisible)
                    .findFirst()
                    .orElse(null);
            final ViewContainer clickedContainer = event.getClickedInventory() instanceof PlayerInventory
                    ? viewer.getSelfContainer()
                    : context.getContainer();

            final RootView root = context.getRoot();
            final IFSlotClickContext clickContext = root.getElementFactory()
                    .createSlotClickContext(
                            event.getRawSlot(), viewer, clickedContainer, clickedComponent, event, false);

            root.getPipeline().execute(StandardPipelinePhases.CLICK, clickContext);
        } catch (Exception e) {
            // TODO custom error handling to customize on error behavior
            viewFrame
                    .getOwner()
                    .getLogger()
                    .severe("An error occurred while processing an inventory click event: " + e.getMessage());
            player.closeInventory();
        }
    }

    @SuppressWarnings("unused")
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInventoryClose(final InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        final Inventory inventory = event.getInventory();
        if (inventory.getType() != InventoryType.ANVIL /* Anvil Input support */
                && !(event.getInventory().getHolder() instanceof IFRenderContext)) return;

        final Player player = (Player) event.getPlayer();
        final Viewer viewer = viewFrame.getViewer(player);
        if (viewer == null) return;

        final IFRenderContext context = viewer.getCurrentContext();
        final RootView root = context.getRoot();
        final IFCloseContext closeContext = root.getElementFactory().createCloseContext(viewer, context, event);

        root.getPipeline().execute(StandardPipelinePhases.CLOSE, closeContext);
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onItemPickup(PlayerPickupItemEvent event) {
        final Viewer viewer = viewFrame.getViewer(event.getPlayer());
        if (viewer == null) return;

        final IFContext context = viewer.getActiveContext();
        if (!context.getConfig().isOptionSet(ViewConfig.CANCEL_ON_PICKUP)) return;

        event.setCancelled(context.getConfig().getOptionValue(ViewConfig.CANCEL_ON_PICKUP));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        final Viewer viewer = viewFrame.getViewer(event.getPlayer());
        if (viewer == null) return;

        final IFContext context = viewer.getActiveContext();
        if (!context.getConfig().isOptionSet(ViewConfig.CANCEL_ON_DROP)) return;

        event.setCancelled(context.getConfig().getOptionValue(ViewConfig.CANCEL_ON_DROP));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        final Viewer viewer = viewFrame.getViewer((Player) event.getWhoClicked());
        if (viewer == null) return;

        final IFContext context = viewer.getActiveContext();
        if (!context.getConfig().isOptionSet(ViewConfig.CANCEL_ON_DRAG)) return;

        final boolean configValue = context.getConfig().getOptionValue(ViewConfig.CANCEL_ON_DRAG);
        final int size = event.getInventory().getSize();
        for (final int rawSlot : event.getRawSlots()) {
            if (!(rawSlot < size)) continue;

            event.setCancelled(configValue);
            break;
        }
    }
}
