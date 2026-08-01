package me.devnatan.inventoryframework.minestom

import me.devnatan.inventoryframework.IFDebug
import me.devnatan.inventoryframework.IFViewFrame
import me.devnatan.inventoryframework.Viewer
import me.devnatan.inventoryframework.context.EndlessContextInfo
import me.devnatan.inventoryframework.feature.DefaultFeatureInstaller
import me.devnatan.inventoryframework.feature.Feature
import me.devnatan.inventoryframework.feature.FeatureInstaller
import me.devnatan.inventoryframework.internal.MinestomElementFactory
import me.devnatan.inventoryframework.internal.PlatformUtils
import net.minestom.server.entity.Player
import net.minestom.server.event.EventNode
import net.minestom.server.event.trait.EntityEvent
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.ApiStatus.Experimental
import java.util.function.UnaryOperator

class ViewFrame private constructor(
    private val parentNode: EventNode<in EntityEvent>,
) : IFViewFrame<ViewFrame, View>() {
    private val featureInstaller: FeatureInstaller<ViewFrame> = DefaultFeatureInstaller(this)

    // region Opening

    /**
     * Opens a view to a player.
     *
     * @param viewClass The target view to be opened.
     * @param player The player that the view will be open to.
     * @return The id of the newly created [IFContext].
     */
    fun open(
        viewClass: Class<out View>,
        player: Player,
    ): String? = open(viewClass, player, null)

    /**
     * Opens a view to a player with initial data.
     *
     * @param viewClass The target view to be opened.
     * @param player The player that the view will be open to.
     * @param initialData The initial data.
     * @return The id of the newly created [IFContext].
     */
    fun open(
        viewClass: Class<out View>,
        player: Player,
        initialData: Any?,
    ): String? = open(viewClass, listOf(player), initialData)

    /**
     * Opens a view to more than one player.
     *
     * These players will see the same inventory and share the same context.
     *
     * *** This API is experimental and is not subject to the general compatibility guarantees such
     * API may be changed or may be removed completely in any further release. ***
     *
     * @param viewClass The target view to be opened.
     * @param players The players that the view will be open to.
     * @return The id of the newly created [IFContext].
     */
    @Experimental
    fun open(
        viewClass: Class<out View>,
        players: Collection<Player>,
    ): String? = open(viewClass, players, null)

    /**
     * Opens a view to more than one player with initial data.
     *
     * These players will see the same inventory and share the same context.
     *
     * *** This API is experimental and is not subject to the general compatibility guarantees such
     * API may be changed or may be removed completely in any further release. ***
     *
     * @param viewClass The target view to be opened.
     * @param players The players that the view will be open to.
     * @param initialData The initial data.
     * @return The id of the newly created [IFContext].
     */
    @Experimental
    fun open(
        viewClass: Class<out View>,
        players: Collection<Player>,
        initialData: Any?,
    ): String? = internalOpen(viewClass, players.associateBy { it.uuid.toString() }, initialData)

    /**
     * Opens an already active context to a player.
     *
     * *** This API is experimental and is not subject to the general compatibility guarantees such
     * API may be changed or may be removed completely in any further release. ***
     *
     * @param contextId The id of the context.
     * @param player Who the context will be open to.
     */
    @Experimental
    fun openActive(
        viewClass: Class<out View>,
        contextId: String,
        player: Player,
    ) {
        openActive(viewClass, contextId, player, null)
    }

    /**
     * Opens an already active context to a player.
     *
     * *** This API is experimental and is not subject to the general compatibility guarantees such
     * API may be changed or may be removed completely in any further release. ***
     *
     * @param contextId The id of the context.
     * @param player Who the context will be open to.
     * @param initialData Initial data to pass to [PlatformView.onViewerAdded].
     */
    @Experimental
    fun openActive(
        viewClass: Class<out View>,
        contextId: String,
        player: Player,
        initialData: Any?,
    ) {
        internalOpenActiveContext(viewClass, contextId, player, initialData)
    }

    /**
     * Opens an already active context to a player.
     *
     * *** This API is experimental and is not subject to the general compatibility guarantees such
     * API may be changed or may be removed completely in any further release. ***
     *
     * @param endlessContextInfo The id of the context.
     * @param player Who the context will be open to.
     */
    @Experimental
    fun openEndless(
        endlessContextInfo: EndlessContextInfo,
        player: Player,
    ) {
        openEndless(endlessContextInfo, player, null)
    }

    /**
     * Opens an already active context to a player.
     *
     * *** This API is experimental and is not subject to the general compatibility guarantees such
     * API may be changed or may be removed completely in any further release. ***
     *
     * @param endlessContextInfo The id of the context.
     * @param player Who the context will be open to.
     * @param initialData Initial data to pass to [PlatformView.onViewerAdded].
     */
    @Experimental
    fun openEndless(
        endlessContextInfo: EndlessContextInfo,
        player: Player,
        initialData: Any?,
    ) {
        openActive(
            endlessContextInfo.view.javaClass as Class<out View>,
            endlessContextInfo.contextId,
            player,
            initialData,
        )
    }

    // endregion
    override fun register(): ViewFrame {
        check(!isRegistered) { "This view frame is already registered" }

        isRegistered = true
        PlatformUtils.setFactory(MinestomElementFactory())
        pipeline.execute(FRAME_REGISTERED, this)
        initializeViews()
        IFInventoryListener(this, parentNode)
        return this
    }

    override fun unregister() {
        if (!isRegistered) return

        // Locks new operations while unregistering
        isRegistered = false

        val iterator: MutableIterator<View> = registeredViews.values.iterator()
        while (iterator.hasNext()) {
            val view = iterator.next()
            try {
                view.closeForEveryone()
            } catch (ignored: RuntimeException) {
            }
            iterator.remove()
        }
        pipeline.execute(FRAME_UNREGISTERED, this)
    }

    private fun initializeViews() {
        for ((_, view) in getRegisteredViews()) {
            try {
                view.internalInitialization(this)
                view.isInitialized = true
            } catch (exception: RuntimeException) {
                view.isInitialized = false
                LOGGER.severe(
                    String.format(
                        "An error occurred while enabling view %s: %s",
                        view.javaClass.name,
                        exception,
                    ),
                )
                exception.printStackTrace()
            }
        }
    }

    // endregion

    /**
     * *** This is an internal inventory-framework API that should not be used from outside of this
     * library. No compatibility guarantees are provided. ***
     */
    @ApiStatus.Internal fun getViewer(player: Player): Viewer? = viewerById[player.uuid.toString()]

    /**
     * Installs a feature.
     *
     * @param feature The feature to be installed.
     * @param configure The feature configuration.
     * @param <C> The feature configuration type.
     * @param <R> The feature value instance type.
     * @return An instance of the installed feature. </R></C>
     */
    fun <C, R> install(
        feature: Feature<C, R, ViewFrame>,
        configure: UnaryOperator<C>,
    ): ViewFrame {
        featureInstaller.install(feature, configure)
        IFDebug.debug("Feature %s installed", feature.name())
        return this
    }

    /**
     * Installs a feature with no specific configuration.
     *
     * @param feature The feature to be installed.
     * @return This view frame.
     */
    fun install(feature: Feature<*, *, ViewFrame>): ViewFrame {
        install(feature, UnaryOperator.identity())
        return this
    }

    /**
     * Disables bStats metrics tracking.
     *
     * InventoryFramework use bStats metrics to obtain some information from servers that use it as
     * a library, such as: number of players, version, software, etc.
     *
     * **No sensitive information is tracked.**
     *
     * @return This view frame.
     */
    fun disableMetrics(): ViewFrame {
        System.setProperty(
            BSTATS_SYSTEM_PROP,
            java.lang.Boolean.FALSE
                .toString(),
        )
        return this
    }

    companion object {
        private const val BSTATS_SYSTEM_PROP = "inventory-framework.enable-bstats"

        init {
            PlatformUtils.setFactory(MinestomElementFactory())
        }

        /**
         * Creates a new ViewFrame.
         *
         * @param owner The plugin that owns this view frame.
         * @return A new ViewFrame instance.
         */
        fun create(parentNode: EventNode<in EntityEvent>): ViewFrame = ViewFrame(parentNode)

        private val LOGGER =
            java.util.logging.Logger
                .getLogger("IF")
    }
}
