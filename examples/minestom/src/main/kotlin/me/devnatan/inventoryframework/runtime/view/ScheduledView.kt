package me.devnatan.inventoryframework.runtime.view

import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.minestom.View
import me.devnatan.inventoryframework.minestom.context.Context
import me.devnatan.inventoryframework.minestom.context.RenderContext
import me.devnatan.inventoryframework.minestom.context.SlotClickContext
import me.devnatan.inventoryframework.runtime.ExampleUtil
import net.minestom.server.item.Material

class ScheduledView : View() {
    val counter = mutableState(0)

    override fun onInit(config: ViewConfigBuilder): Unit =
        with(config) {
            cancelOnClick()
            size(3)
            title("Simple Pagination")
            layout("         ", "    C    ", "B        ")
            scheduleUpdate(20)
        }

    override fun onFirstRender(render: RenderContext) {
        render.layoutSlot('C').onRender {
            it.item = ExampleUtil.displayItem(Material.STONE, counter.increment(it).toString())
        }

        render
            .layoutSlot('B', ExampleUtil.displayItem(Material.PAPER, "Back"))
            .displayIf(Context::canBack)
            .onClick(SlotClickContext::back)
    }
}
