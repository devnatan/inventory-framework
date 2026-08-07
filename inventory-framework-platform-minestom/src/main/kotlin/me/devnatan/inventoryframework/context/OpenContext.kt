package me.devnatan.inventoryframework.context

import me.devnatan.inventoryframework.MinestomViewer
import me.devnatan.inventoryframework.View
import me.devnatan.inventoryframework.ViewConfig
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.ViewContainer
import me.devnatan.inventoryframework.Viewer
import me.devnatan.inventoryframework.state.AsyncState
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import org.jetbrains.annotations.ApiStatus
import java.util.Objects
import java.util.UUID
import java.util.concurrent.CompletableFuture

/**
 * Creates a new open context instance.
 *
 * *** This is an internal inventory-framework API that should not be used from outside of this
 * library. No compatibility guarantees are provided. ***
 *
 * @param root Root view that will be owner of the upcoming render context.
 * @param subject The viewer that is opening the view.
 * @param viewers Who'll be the viewers of this context, if this parameter is provided it means that
 *   this context is a shared context. Must be provided even in non-shared context cases.
 * @param initialData Initial data provided by the user.
 */
class OpenContext
    @ApiStatus.Internal
    constructor(
        private val root: View,
        private val subject: Viewer?,
        private val viewers: Map<String, Viewer>,
        private var initialData: Any?,
    ) : PlatformConfinedContext(),
        IFOpenContext,
        Context {
        private var container: ViewContainer? = null

        // --- Inherited ---

        private val id: UUID = UUID.randomUUID()

        // --- User Provided ---

        private var waitTask: CompletableFuture<Void>? = null
        private var waitState: AsyncState<*>? = null
        private var inheritedConfigBuilder: ViewConfigBuilder? = null

        // --- Properties ---

        /**
         * The player that's currently opening the view.
         *
         * @return The player that is opening the view.
         * @throws UnsupportedOperationInSharedContextException If this context [is shared][.isShared].
         */
        override val player: Player
            get() {
                tryThrowDoNotWorkWithSharedContext("getAllPlayers()")
                return field
            }

        private var cancelled = false

        init {
            this.initialData = initialData
            this.player = (subject as MinestomViewer).player
        }

        override val allPlayers: List<Player>
            get() = getViewers().stream().map { viewer -> (viewer as MinestomViewer).player }.toList()

        override fun updateTitleForPlayer(
            title: Component,
            player: Player,
        ) {
            tryThrowDoNotWorkWithSharedContext()
            modifyConfig().title(title)
        }

        override fun resetTitleForPlayer(player: Player) {
            tryThrowDoNotWorkWithSharedContext()
            if (modifiedConfig == null) return

            modifyConfig().title(null)
        }

        override fun isCancelled(): Boolean = cancelled

        override fun setCancelled(cancelled: Boolean) {
            this.cancelled = cancelled
        }

        override fun getAsyncOpenJob(): CompletableFuture<Void>? = waitTask

        override fun getRoot(): View = root

        override fun getIndexedViewers(): Map<String, Viewer> = viewers

        override fun getId(): UUID = id

        override fun getInitialData(): Any? = initialData

        override fun setInitialData(initialData: Any?) {
            this.initialData = initialData
        }

        override fun waitUntil(task: CompletableFuture<Void>) {
            this.waitTask = task
        }

        override fun waitFor(state: AsyncState<*>) {
            this.waitState = state
        }

        override fun getConfig(): ViewConfig =
            if (inheritedConfigBuilder == null) {
                getRoot().config
            } else {
                Objects.requireNonNull<ViewConfig>(modifiedConfig, "Modified config cannot be null")
            }

        override fun getModifiedConfig(): ViewConfig? {
            if (inheritedConfigBuilder == null) return null

            return inheritedConfigBuilder!!.build().merge(getRoot().config)
        }

        override fun modifyConfig(): ViewConfigBuilder {
            if (inheritedConfigBuilder == null) inheritedConfigBuilder = ViewConfigBuilder()

            return inheritedConfigBuilder!!
        }

        override fun getViewer(): Viewer? {
            tryThrowDoNotWorkWithSharedContext("getViewers()")
            return subject
        }

        override fun getContainer(): ViewContainer? = container

        override fun setContainer(container: ViewContainer) {
            this.container = container
        }
    }
