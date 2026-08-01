package me.devnatan.inventoryframework.minestom.context

import me.devnatan.inventoryframework.MinestomViewer
import me.devnatan.inventoryframework.ViewConfig
import me.devnatan.inventoryframework.ViewContainer
import me.devnatan.inventoryframework.Viewer
import me.devnatan.inventoryframework.context.IFCloseContext
import me.devnatan.inventoryframework.context.IFRenderContext
import me.devnatan.inventoryframework.context.PlatformConfinedContext
import me.devnatan.inventoryframework.minestom.View
import me.devnatan.inventoryframework.state.State
import me.devnatan.inventoryframework.state.StateValue
import me.devnatan.inventoryframework.state.StateWatcher
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.event.inventory.InventoryCloseEvent
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.UnmodifiableView
import java.util.UUID

class CloseContext
    @ApiStatus.Internal
    constructor(
        subject: Viewer,
        private val parent: IFRenderContext,
        val origin: InventoryCloseEvent,
    ) : PlatformConfinedContext(),
        IFCloseContext,
        Context {
        private val subject: Viewer = subject
        override val player: Player = (subject as MinestomViewer).player

        private var cancelled = false

        override fun getPlatformEvent(): Any = origin

        override val allPlayers: List<Player>
            get() = getParent().allPlayers

        override fun updateTitleForPlayer(
            title: Component,
            player: Player,
        ) {
            getParent().updateTitleForPlayer(title, player)
        }

        override fun resetTitleForPlayer(player: Player) {
            getParent().resetTitleForPlayer(player)
        }

        override fun isCancelled(): Boolean = cancelled

        override fun setCancelled(cancelled: Boolean) {
            this.cancelled = cancelled
        }

        override fun getViewer(): Viewer = subject

        override fun getParent(): RenderContext = parent as RenderContext

        override fun getId(): UUID = getParent().id

        override fun getConfig(): ViewConfig = getParent().config

        override fun getContainer(): ViewContainer = getParent().container

        override fun getRoot(): View = getParent().getRoot()

        override fun getInitialData(): Any = getParent().initialData

        override fun setInitialData(initialData: Any) {
            getParent().initialData = initialData
        }

        override fun getStateValues(): @UnmodifiableView MutableMap<Long, StateValue>? = getParent().stateValues

        override fun initializeState(
            id: Long,
            value: StateValue,
        ) {
            getParent().initializeState(id, value)
        }

        override fun watchState(
            id: Long,
            listener: StateWatcher,
        ) {
            getParent().watchState(id, listener)
        }

        override fun getRawStateValue(state: State<*>?): Any = getParent().getRawStateValue(state)

        override fun getInternalStateValue(state: State<*>): StateValue = getParent().getInternalStateValue(state)

        override fun getUninitializedStateValue(stateId: Long): StateValue = getParent().getUninitializedStateValue(stateId)

        override fun updateState(
            state: State<*>,
            value: Any,
        ) {
            getParent().updateState(state, value)
        }

        override fun toString(): String =
            (
                "CloseContext{" +
                    "subject=" +
                    subject +
                    ", player=" +
                    player +
                    ", parent=" +
                    parent +
                    ", cancelled=" +
                    cancelled +
                    "} " +
                    super.toString()
            )
    }
