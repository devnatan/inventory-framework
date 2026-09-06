package me.devnatan.inventoryframework.runtime.view;

import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class AllowsEntityContainerInteractions extends View {

    @Override
    public void onInit(@NotNull ViewConfigBuilder config) {
        config.title("Allows player inventory interactions")
			.size(3)
			.cancelOnClick()
			.allowEntityContainerInteractions();
    }

	@Override
	public void onOpen(@NonNull OpenContext open) {
		open.getPlayer().getInventory().addItem(new ItemStack(Material.WOODEN_PICKAXE));
	}

	@Override
    public void onFirstRender(@NotNull RenderContext render) {
		render.firstSlot(new ItemStack(Material.GOLD_INGOT));
    }
}
