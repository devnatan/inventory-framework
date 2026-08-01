package me.devnatan.inventoryframework.runtime.view;

import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.bukkit.View;
import me.devnatan.inventoryframework.bukkit.context.Context;
import me.devnatan.inventoryframework.bukkit.context.RenderContext;
import me.devnatan.inventoryframework.state.MutableIntState;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class AutoUpdate extends View {

    private final MutableIntState countState = mutableState(0);

    @Override
    public void onInit(@NotNull ViewConfigBuilder config) {
        config.cancelOnClick().title("Auto update (?)").scheduleUpdate(10);
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render) {
        render.slot(1, new ItemStack(Material.DIAMOND)).onClick(click -> click.openForPlayer(SimplePagination.class));
    }

    @Override
    public void onUpdate(@NotNull Context update) {
        final int count = countState.increment(update);
        update.updateTitleForPlayer("Auto update (" + count + ")");
    }
}
