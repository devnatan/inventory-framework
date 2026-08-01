package me.devnatan.inventoryframework.component

import me.devnatan.inventoryframework.Ref
import me.devnatan.inventoryframework.ViewContainer
import me.devnatan.inventoryframework.VirtualView
import me.devnatan.inventoryframework.context.IFContext
import me.devnatan.inventoryframework.context.IFRenderContext
import me.devnatan.inventoryframework.context.IFSlotClickContext
import me.devnatan.inventoryframework.context.IFSlotContext
import me.devnatan.inventoryframework.context.IFSlotRenderContext
import me.devnatan.inventoryframework.minestom.context.Context
import me.devnatan.inventoryframework.minestom.context.SlotClickContext
import me.devnatan.inventoryframework.minestom.context.SlotContext
import me.devnatan.inventoryframework.minestom.context.SlotRenderContext
import me.devnatan.inventoryframework.state.State
import me.devnatan.inventoryframework.utils.SlotConverter
import net.minestom.server.item.ItemStack
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier

class MinestomItemComponentBuilder
    private constructor(
        private val root: VirtualView,
        keyFactory: Function<out IFContext, String>?,
        slot: Int,
        item: ItemStack?,
        renderHandler: Consumer<in IFSlotRenderContext>?,
        clickHandler: Consumer<in IFSlotClickContext>?,
        updateHandler: Consumer<in IFSlotContext>?,
        reference: Ref<Component>?,
        data: Map<String, Any>,
        cancelOnClick: Boolean,
        closeOnClick: Boolean,
        updateOnClick: Boolean,
        watchingStates: Set<State<*>>,
        isManagedExternally: Boolean,
        displayCondition: Predicate<Context>?,
    ) : DefaultComponentBuilder<MinestomItemComponentBuilder, Context>(
            keyFactory,
            reference,
            data,
            cancelOnClick,
            closeOnClick,
            updateOnClick,
            watchingStates,
            isManagedExternally,
            displayCondition,
        ),
        ItemComponentBuilder<MinestomItemComponentBuilder, Context>,
        ComponentFactory {
        private var slot: Int
        private var item: ItemStack?
        private var renderHandler: Consumer<in IFSlotRenderContext>?
        private var clickHandler: Consumer<in IFSlotClickContext>?
        private var updateHandler: Consumer<in IFSlotContext>?

        constructor(
            root: VirtualView,
        ) : this(
            root = root,
            keyFactory = null,
            slot = -1,
            item = null,
            renderHandler = null,
            clickHandler = null,
            updateHandler = null,
            reference = null,
            data = HashMap<String, Any>(),
            cancelOnClick = false,
            closeOnClick = false,
            updateOnClick = false,
            watchingStates = LinkedHashSet<State<*>>(),
            isManagedExternally = false,
            displayCondition = null,
        )

        init {
            this.slot = slot
            this.item = item
            this.renderHandler = renderHandler
            this.clickHandler = clickHandler
            this.updateHandler = updateHandler
        }

        override fun toString(): String =
            (
                "BukkitItemComponentBuilder{" +
                    "slot=" +
                    slot +
                    ", item=" +
                    item +
                    ", renderHandler=" +
                    renderHandler +
                    ", clickHandler=" +
                    clickHandler +
                    ", updateHandler=" +
                    updateHandler +
                    "} " +
                    super.toString()
            )

        override fun isContainedWithin(position: Int): Boolean = position == slot

        /** {@inheritDoc} */
        override fun withSlot(slot: Int): MinestomItemComponentBuilder {
            this.slot = slot
            return this
        }

        override fun withSlot(
            row: Int,
            column: Int,
        ): MinestomItemComponentBuilder {
            val container: ViewContainer = (root as IFRenderContext).getContainer()
            return withSlot(
                SlotConverter.convertSlot(
                    row,
                    column,
                    container.getRowsCount(),
                    container.getColumnsCount(),
                ),
            )
        }

        /**
         * Defines the item that will be used as fallback for rendering in the slot where this item is
         * positioned. The fallback item is always static.
         *
         * @param item The new fallback item stack.
         * @return This item builder.
         */
        fun withItem(item: ItemStack?): MinestomItemComponentBuilder {
            this.item = item
            return this
        }

        /**
         * Called when the item is rendered.
         *
         * This handler is called every time the item or the view that owns it is updated.
         *
         * @param renderHandler The render handler.
         * @return This item builder.
         */
        @Suppress("UNCHECKED_CAST")
        fun onRender(renderHandler: Consumer<in SlotRenderContext>?): MinestomItemComponentBuilder {
            this.renderHandler = renderHandler as? Consumer<in IFSlotRenderContext>
            return this
        }

        /**
         * Dynamic rendering of a specific item.
         *
         * This handler is called every time the item or the view that owns it is updated.
         *
         * @param renderFactory The render handler.
         * @return This item builder.
         */
        fun renderWith(renderFactory: Supplier<ItemStack>): MinestomItemComponentBuilder =
            onRender { render: SlotRenderContext ->
                render.item = renderFactory.get()
            }

        /**
         * Called when a player clicks on the item.
         *
         * This handler works on any container that the actor has access to and only works if the
         * interaction has not been cancelled.
         *
         * @param clickHandler The click handler.
         * @return This item builder.
         */
        @Suppress("UNCHECKED_CAST")
        fun onClick(clickHandler: Consumer<in SlotClickContext>?): MinestomItemComponentBuilder {
            this.clickHandler = clickHandler as? Consumer<in IFSlotClickContext>
            return this
        }

        /**
         * Called when a player clicks on the item.
         *
         * This handler works on any container that the actor has access to and only works if the
         * interaction has not been cancelled.
         *
         * @param clickHandler The click handler.
         * @return This item builder.
         */
        fun onClick(clickHandler: Runnable?): MinestomItemComponentBuilder =
            onClick(
                if (clickHandler == null) {
                    null
                } else {
                    Consumer { `$`: SlotClickContext? -> clickHandler.run() }
                },
            )

        /**
         * Called when the item is updated.
         *
         * @param updateHandler The update handler.
         * @return This item builder.
         */
        @Suppress("UNCHECKED_CAST")
        fun onUpdate(updateHandler: Consumer<SlotContext>?): MinestomItemComponentBuilder {
            this.updateHandler = updateHandler as? Consumer<in IFSlotContext>
            return this
        }

        override fun create(): Component {
            val componentKeyProvider = if (keyFactory == null) RANDOM_KEY_FACTORY else keyFactory

            return ItemComponent(
                // keyFactory =
                componentKeyProvider,
                // root =
                root,
                // position =
                slot,
                // stack =
                item,
                // cancelOnClick =
                cancelOnClick,
                // closeOnClick =
                closeOnClick,
                // displayCondition =
                displayCondition,
                // renderHandler =
                renderHandler,
                // updateHandler =
                updateHandler,
                // clickHandler =
                clickHandler,
                // watching =
                watchingStates,
                // isManagedExternally =
                isManagedExternally,
                // updateOnClick =
                updateOnClick,
                // isVisible =
                false,
                // reference =
                reference,
            )
        }

        override fun copy(): MinestomItemComponentBuilder =
            MinestomItemComponentBuilder(
                root = root,
                keyFactory = keyFactory,
                slot = slot,
                item = item,
                renderHandler = renderHandler,
                clickHandler = clickHandler,
                updateHandler = updateHandler,
                reference = reference,
                data = data,
                cancelOnClick = cancelOnClick,
                closeOnClick = closeOnClick,
                updateOnClick = updateOnClick,
                watchingStates = watchingStates,
                isManagedExternally = isManagedExternally,
                displayCondition = displayCondition,
            )
    }
