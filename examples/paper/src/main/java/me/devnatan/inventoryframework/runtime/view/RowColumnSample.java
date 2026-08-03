package me.devnatan.inventoryframework.runtime.view;

import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.MutableIntState;
import me.devnatan.inventoryframework.state.MutableState;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class RowColumnSample extends View {

	private final MutableIntState intState = mutableState(0);

    @Override
    public void onInit(@NotNull ViewConfigBuilder config) {
        config.cancelOnClick().title("Row & Column").size(6);
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render) {
		render.slot(13)
			.withItem(intState.get(render) == 3 ? new ItemStack(Material.GOLD_INGOT) : new ItemStack(Material.IRON_INGOT));

		render.firstRow((pos, slot) ->
			slot.withItem(new ItemStack(Material.BLUE_STAINED_GLASS_PANE))
				.onClick(click -> intState.increment(click))
		);

		render.lastColumn((pos, slot) ->
			slot.withItem(new ItemStack(Material.BLACK_STAINED_GLASS_PANE))
		);
    }
}
