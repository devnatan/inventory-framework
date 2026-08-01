package me.devnatan.inventoryframework.runtime.view

import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.minestom.View
import me.devnatan.inventoryframework.minestom.context.RenderContext
import me.devnatan.inventoryframework.minestom.context.SlotClickContext
import me.devnatan.inventoryframework.minestom.context.SlotRenderContext
import me.devnatan.inventoryframework.runtime.ExampleUtil.displayItem
import me.devnatan.inventoryframework.state.MutableState
import net.minestom.server.item.Material

class Failing : View() {
    var state: MutableState<Int> = mutableState(0)

    override fun onInit(config: ViewConfigBuilder): Unit =
        with(config) {
            size(1)
            cancelOnClick()
            title("Failing Inventory")
            layout("  R   C  ")
        }

    override fun onFirstRender(render: RenderContext) {
        render
            .layoutSlot('R')
            .onRender { ctx: SlotRenderContext ->
                if (state[ctx] == 0) {
                    ctx.item = displayItem(Material.DIAMOND, "Click me to fail")
                } else {
                    throw IllegalStateException("This item cannot be rendered")
                }
            }.onClick { ctx: SlotClickContext ->
                state[1] = ctx
                ctx.update()
            }

        render.layoutSlot('C', displayItem(Material.STONE, "Click me and I will fail")).onClick { _ ->
            throw IllegalStateException("This is a failing inventory")
        }
    }
}
