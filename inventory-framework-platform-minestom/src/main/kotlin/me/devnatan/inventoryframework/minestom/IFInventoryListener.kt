package me.devnatan.inventoryframework.minestom

import me.devnatan.inventoryframework.RootView
import me.devnatan.inventoryframework.ViewConfig
import me.devnatan.inventoryframework.context.IFCloseContext
import me.devnatan.inventoryframework.context.IFContext
import me.devnatan.inventoryframework.context.IFRenderContext
import me.devnatan.inventoryframework.context.IFSlotClickContext
import me.devnatan.inventoryframework.pipeline.StandardPipelinePhases
import net.minestom.server.entity.Player
import net.minestom.server.event.EventFilter
import net.minestom.server.event.EventNode
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.item.PickupItemEvent
import net.minestom.server.event.trait.EntityEvent
import net.minestom.server.inventory.PlayerInventory
import net.minestom.server.inventory.click.Click
import kotlin.jvm.optionals.getOrNull

internal class IFInventoryListener(
    private val viewFrame: ViewFrame,
    handler: EventNode<in EntityEvent>,
) {
    init {
        val node =
            EventNode
                .type("IF", EventFilter.ENTITY) { _, e ->
                    e is Player && viewFrame.getViewer(e) != null
                }.setPriority(10)
                .addListener(PickupItemEvent::class.java, this::onItemPickup)
                .addListener(InventoryPreClickEvent::class.java, this::onInventoryClick)
                .addListener(InventoryCloseEvent::class.java, this::onInventoryClose)
        handler.addChild(node)
    }

    fun onInventoryClick(event: InventoryPreClickEvent) {
        if (event.isCancelled) return

        val player = event.player
        val viewer = viewFrame.getViewer(player) ?: return

        if (event.click is Click.DropSlot) {
            val context: IFContext = viewer.activeContext
            if (!context.config.isOptionSet(ViewConfig.CANCEL_ON_DROP)) return

            event.isCancelled = context.config.getOptionValue(ViewConfig.CANCEL_ON_DROP)
            return
        }
        if (event.click is Click.LeftDrag || event.click is Click.RightDrag) {
            val context: IFContext = viewer.activeContext
            if (!context.config.isOptionSet(ViewConfig.CANCEL_ON_DRAG)) return

            event.isCancelled = context.config.getOptionValue(ViewConfig.CANCEL_ON_DRAG)
            return
        }

        val context: IFRenderContext = viewer.activeContext
        val clickedComponent =
            context
                .getComponentsAt(event.slot)
                .stream()
                .filter { it.isVisible }
                .findFirst()
                .getOrNull()
        val clickedContainer =
            if (event.inventory is PlayerInventory) {
                viewer.selfContainer
            } else {
                context.getContainer()
            }

        val root: RootView = context.getRoot()
        val clickContext: IFSlotClickContext =
            root.elementFactory.createSlotClickContext(
                event.slot,
                viewer,
                clickedContainer,
                clickedComponent,
                event,
                false,
            )

        root.pipeline.execute(StandardPipelinePhases.CLICK, clickContext)
    }

    fun onInventoryClose(event: InventoryCloseEvent) {
        val player: Player = event.player
        val viewer = viewFrame.getViewer(player) ?: return

        val context: IFRenderContext = viewer.activeContext
        val root: RootView = context.getRoot()
        val closeContext: IFCloseContext =
            root.elementFactory.createCloseContext(viewer, context, event)

        root.pipeline.execute(StandardPipelinePhases.CLOSE, closeContext)
    }

    fun onItemPickup(event: PickupItemEvent) {
        val viewer = viewFrame.getViewer(event.entity as Player) ?: return

        val context: IFContext = viewer.activeContext
        if (!context.getConfig().isOptionSet(ViewConfig.CANCEL_ON_PICKUP)) return

        event.isCancelled = context.getConfig().getOptionValue(ViewConfig.CANCEL_ON_PICKUP)
    }
}
