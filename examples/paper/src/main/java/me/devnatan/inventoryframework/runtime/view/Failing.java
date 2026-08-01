package me.devnatan.inventoryframework.runtime.view;

import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.bukkit.View;
import me.devnatan.inventoryframework.bukkit.context.RenderContext;
import me.devnatan.inventoryframework.runtime.ExampleUtil;
import me.devnatan.inventoryframework.state.MutableState;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public class Failing extends View {

    MutableState<Integer> state = mutableState(0);

    @Override
    public void onInit(@NotNull ViewConfigBuilder config) {
        config.size(1);
        config.cancelOnClick();
        config.title("Failing Inventory");
        config.layout("  R   C  ");
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render) {
        render.layoutSlot('R')
                .onRender((ctx) -> {
                    if (state.get(ctx) == 0) {
                        ctx.setItem(ExampleUtil.displayItem(Material.DIAMOND, "Click me to fail"));
                    } else {
                        throw new IllegalStateException("This item cannot be rendered");
                    }
                })
                .onClick((ctx) -> {
                    state.set(1, ctx);
                    ctx.update();
                });

        render.layoutSlot('C', ExampleUtil.displayItem(Material.STONE, "Click me and I will fail"))
                .onClick((ctx) -> {
                    throw new IllegalStateException("This is a failing inventory");
                });
    }
}
