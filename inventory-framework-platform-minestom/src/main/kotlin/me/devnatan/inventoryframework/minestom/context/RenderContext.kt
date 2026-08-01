package me.devnatan.inventoryframework.minestom.context

import me.devnatan.inventoryframework.MinestomViewContainer
import me.devnatan.inventoryframework.MinestomViewer
import me.devnatan.inventoryframework.ViewConfig
import me.devnatan.inventoryframework.ViewContainer
import me.devnatan.inventoryframework.Viewer
import me.devnatan.inventoryframework.component.MinestomItemComponentBuilder
import me.devnatan.inventoryframework.context.PlatformRenderContext
import me.devnatan.inventoryframework.minestom.View
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import org.jetbrains.annotations.ApiStatus
import java.util.Objects
import java.util.UUID

class RenderContext
    @ApiStatus.Internal
    constructor(
        id: UUID,
        root: View,
        config: ViewConfig,
        container: ViewContainer?,
        viewers: Map<String, Viewer>,
        subject: Viewer,
        initialData: Any?,
    ) : PlatformRenderContext<MinestomItemComponentBuilder, Context>(
            id,
            root,
            config,
            container,
            viewers,
            subject,
            initialData,
        ),
        Context {
        override val player: Player = (subject as MinestomViewer).player
            get() {
                tryThrowDoNotWorkWithSharedContext("getAllPlayers")
                return field
            }

        override fun getRoot(): View = root as View

        override val allPlayers: List<Player>
            get() = viewers.stream().map { viewer -> (viewer as MinestomViewer).player }.toList()

        override fun updateTitleForPlayer(
            title: Component,
            player: Player,
        ) {
            (container as MinestomViewContainer).changeTitle(title, player)
        }

        override fun resetTitleForPlayer(player: Player) {
            (container as MinestomViewContainer).changeTitle(Component.empty(), player)
        }

        /**
         * Adds an item to a specific slot in the context container.
         *
         * @param slot The slot in which the item will be positioned.
         * @return An item builder to configure the item.
         */
        fun slot(
            slot: Int,
            item: ItemStack,
        ): MinestomItemComponentBuilder = slot(slot).withItem(item)

        /**
         * Adds an item at the specific column and ROW (X, Y) in that context's container.
         *
         * @param row The row (Y) in which the item will be positioned.
         * @param column The column (X) in which the item will be positioned.
         * @return An item builder to configure the item.
         */
        fun slot(
            row: Int,
            column: Int,
            item: ItemStack?,
        ): MinestomItemComponentBuilder = slot(row, column).withItem(item)

        /**
         * Sets an item in the first slot of this context's container.
         *
         * @param item The item that'll be set.
         * @return An item builder to configure the item.
         */
        fun firstSlot(item: ItemStack?): MinestomItemComponentBuilder = firstSlot().withItem(item)

        /**
         * Sets an item in the last slot of this context's container.
         *
         * @param item The item that'll be set.
         * @return An item builder to configure the item.
         */
        fun lastSlot(item: ItemStack?): MinestomItemComponentBuilder = lastSlot().withItem(item)

        /**
         * Adds an item in the next available slot of this context's container.
         *
         * @param item The item that'll be added.
         * @return An item builder to configure the item.
         */
        fun availableSlot(item: ItemStack?): MinestomItemComponentBuilder = availableSlot().withItem(item)

        /**
         * Defines the item that will represent a character provided in the context layout.
         *
         * @param character The layout character target.
         * @param item The item that'll represent the layout character.
         * @return An item builder to configure the item.
         */
        fun layoutSlot(
            character: Char,
            item: ItemStack?,
        ): MinestomItemComponentBuilder = layoutSlot(character).withItem(item)

        /**
         * *** This API is experimental and is not subject to the general compatibility guarantees such
         * API may be changed or may be removed completely in any further release. ***
         */
        @ApiStatus.Experimental
        fun resultSlot(item: ItemStack?): MinestomItemComponentBuilder = resultSlot().withItem(item)

        override fun createBuilder(): MinestomItemComponentBuilder = MinestomItemComponentBuilder(this)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            if (!super.equals(other)) return false
            val that = other as RenderContext
            return player == that.player
        }

        override fun hashCode(): Int = Objects.hash(super.hashCode(), player)

        override fun toString(): String = "RenderContext{" + "player=" + player + "} " + super.toString()
    }
