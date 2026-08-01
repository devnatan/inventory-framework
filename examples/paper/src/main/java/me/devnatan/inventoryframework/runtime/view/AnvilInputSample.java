package me.devnatan.inventoryframework.runtime.view;

import me.devnatan.inventoryframework.AnvilInput;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.ViewType;
import me.devnatan.inventoryframework.bukkit.View;
import me.devnatan.inventoryframework.bukkit.context.RenderContext;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class AnvilInputSample extends View {

    final AnvilInput anvilInput = AnvilInput.createAnvilInput();

    @Override
    public void onInit(@NotNull ViewConfigBuilder config) {
        config.cancelOnClick();
        config.type(ViewType.ANVIL);
        config.title("Anvil Input Sample");
        config.use(anvilInput);
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render) {
        ItemStack itemStack = new ItemStack(Material.DIAMOND);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName("Type something here");
        itemStack.setItemMeta(itemMeta);

        render.firstSlot(itemStack);
        render.resultSlot().onClick((ctx) -> {
            ctx.getPlayer().sendMessage("You typed: " + anvilInput.get(ctx));
        });
    }
}
