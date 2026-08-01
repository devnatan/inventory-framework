package me.devnatan.inventoryframework.bukkit.pipeline;

import me.devnatan.inventoryframework.VirtualView;
import me.devnatan.inventoryframework.bukkit.context.CloseContext;
import me.devnatan.inventoryframework.context.IFCloseContext;
import me.devnatan.inventoryframework.pipeline.PipelineContext;
import me.devnatan.inventoryframework.pipeline.PipelineInterceptor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CancelledCloseInterceptor implements PipelineInterceptor<VirtualView> {

    @SuppressWarnings("ConstantValue")
    @Override
    public void intercept(PipelineContext<VirtualView> pipeline, VirtualView subject) {
        if (!(subject instanceof IFCloseContext)) return;

        final CloseContext context = (CloseContext) subject;
        if (!context.isCancelled()) return;

        final Player player = context.getPlayer();
        final ItemStack cursor = player.getItemOnCursor();

        context.getRoot().nextTick(() -> context.getViewer().open(context.getContainer()));

        // suppress cursor null check since it can be null in legacy versions
        if ((cursor != null) && cursor.getType() != Material.AIR) player.setItemOnCursor(cursor);
    }
}
