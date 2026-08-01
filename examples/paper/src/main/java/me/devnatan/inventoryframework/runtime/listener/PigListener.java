package me.devnatan.inventoryframework.runtime.listener;

import me.devnatan.inventoryframework.bukkit.ViewFrame;
import me.devnatan.inventoryframework.runtime.view.SimplePagination;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public final class PigListener implements Listener {

    private final ViewFrame viewFrame;

    public PigListener(ViewFrame viewFrame) {
        this.viewFrame = viewFrame;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getType() != EntityType.PIG) return;

        viewFrame.open(SimplePagination.class, event.getPlayer());
    }
}
