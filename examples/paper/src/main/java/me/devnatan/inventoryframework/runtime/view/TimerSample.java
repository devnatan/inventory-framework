package me.devnatan.inventoryframework.runtime.view;

import java.util.Arrays;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.context.SlotRenderContext;
import me.devnatan.inventoryframework.state.MutableIntState;
import me.devnatan.inventoryframework.state.timer.Timer;
import me.devnatan.inventoryframework.state.timer.TimerState;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class TimerSample extends View {

    private final MutableIntState countState = mutableState(0);
    private final TimerState timerState = timerState(20);

    @Override
    public void onInit(@NotNull ViewConfigBuilder config) {
        config.cancelOnClick().title("Timer (?)").scheduleUpdate(timerState);
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render) {
        render.firstSlot()
                .onRender(this::onClockItemRender)
                .onClick(this::onClockItemClick)
                .updateOnStateChange(timerState);

        render.lastSlot()
                .onRender(this::onIntervalItemRender)
                .onClick(this::onIntervalItemClick)
                .updateOnStateChange(timerState);
    }

    @Override
    public void onUpdate(@NotNull Context update) {
        final int count = countState.increment(update);
        final String pauseSuffix = timerState.get(update).isPaused() ? " [paused]" : "";
        update.updateTitleForPlayer("Timer (" + count + ")" + pauseSuffix);
    }

    private void onClockItemRender(@NotNull SlotRenderContext render) {
        final Timer timer = timerState.get(render);

        final ItemStack item = new ItemStack(Material.CLOCK);
        final ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(timer.isPaused() ? "Click to resume" : "Click to pause");
        item.setItemMeta(meta);
        render.setItem(item);
    }

    private void onClockItemClick(@NotNull SlotClickContext click) {
        final Timer timer = timerState.get(click);
        final boolean paused = timer.pause();
        click.getPlayer().sendMessage(paused ? "Timer paused" : "Timer resumed");
    }

    private void onIntervalItemRender(@NotNull SlotRenderContext render) {
        final Timer timer = timerState.get(render);

        final ItemStack item = new ItemStack(Material.ARROW, (int) Math.max(timer.currentInterval() / 20, 1));
        final ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("Current update interval");
        meta.setLore(Arrays.asList(
                "Initial: " + timer.initialInterval() + " ticks", "Current: " + timer.currentInterval() + " ticks"));
        item.setItemMeta(meta);
        render.setItem(item);
    }

    private void onIntervalItemClick(@NotNull SlotClickContext click) {
        final Timer timer = timerState.get(click);
        final long newInterval = (timer.currentInterval() % 100) + 20;
        timer.changeInterval(newInterval);
        click.getPlayer().sendMessage("Timer interval changed to " + newInterval + " ticks");
    }
}
