package me.devnatan.inventoryframework.runtime.view;

import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.context.RenderContext;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class NavigatingBetweenViewsB extends View {

	@Override
	public void onInit(ViewConfigBuilder config) {
		config.title("B").size(3);
	}

	@Override
	public void onFirstRender(RenderContext render) {
		// Moves player to back to "A" view on click
		render.firstSlot(new ItemStack(Material.REDSTONE))
			.onClick(click -> click.openForPlayer(NavigatingBetweenViewsA.class));
	}
}
