package me.devnatan.inventoryframework.runtime.view;

import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.context.RenderContext;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class NavigatingBetweenViewsA extends View {

    @Override
    public void onInit(ViewConfigBuilder config) {
        config.title("A").size(3);
    }

    @Override
    public void onFirstRender(RenderContext render) {
        // Moves player to "B" view on click
        render.firstSlot(new ItemStack(Material.DIAMOND))
                .onClick(click -> click.openForPlayer(NavigatingBetweenViewsB.class));
    }
}
