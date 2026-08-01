package me.devnatan.inventoryframework.runtime.view;

import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.bukkit.View;
import me.devnatan.inventoryframework.bukkit.context.RenderContext;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class RowColumnSample extends View {

    @Override
    public void onInit(@NotNull ViewConfigBuilder config) {
        config.cancelOnClick().title("Row & Column").size(6);
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render) {
        render.slot(13, new ItemStack(Material.DIAMOND_SWORD));

        render.firstRow((pos, slot) -> slot.withItem(new ItemStack(Material.BLUE_STAINED_GLASS_PANE)));

        render.lastColumn((pos, slot) -> slot.withItem(new ItemStack(Material.BLACK_STAINED_GLASS_PANE)));
    }
}
