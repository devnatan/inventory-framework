package me.devnatan.inventoryframework.runtime.view;

import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.context.RenderContext;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class RowColumnSample extends View {

    @Override
    public void onInit(@NotNull ViewConfigBuilder config) {
        config.cancelOnClick().title("Row & Column").size(4);
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render) {
        final int columnsCount = render.getContainer().getColumnsCount();
        final int rowsCount = render.getContainer().getRowsCount();

        // Fills the whole first row, left to right.
        for (int i = 0; i < columnsCount; i++) {
            render.firstRow().withItem(new ItemStack(Material.LIME_STAINED_GLASS_PANE));
        }

        // Fills the whole last column, top to bottom.
        for (int i = 0; i < rowsCount; i++) {
            render.lastColumn().withItem(new ItemStack(Material.ORANGE_STAINED_GLASS_PANE));
        }
    }
}
