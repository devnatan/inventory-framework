package me.devnatan.inventoryframework.minestom.context

import me.devnatan.inventoryframework.context.IFConfinedContext
import me.devnatan.inventoryframework.context.IFOpenContext
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import org.jetbrains.annotations.ApiStatus.Experimental

interface Context : IFConfinedContext {
    /**
     * The player for the current interaction context.
     *
     * Contexts can be shared and contain multiple viewers, this method will always return the
     * player for the current event.
     *
     * @return A player in this interaction context.
     */
    val player: Player

    @get:Experimental val allPlayers: List<Player>

    /**
     * Updates the container title for a specific player.
     *
     * This should not be used before the container is opened, if you need to set the __initial
     * title__ use [IFOpenContext.modifyConfig] on open handler instead.
     *
     * This method is version dependant, so it may be that your server version is not yet supported,
     * if you try to use this method and fail (can fail silently), report it to the library
     * developers to add support to your version.
     *
     * *** This API is experimental and is not subject to the general compatibility guarantees such
     * API may be changed or may be removed completely in any further release. ***
     *
     * @param title The new container title.
     * @param player The player to update the title.
     */
    @Experimental fun updateTitleForPlayer(
        title: Component,
        player: Player,
    )

    /**
     * Resets the container title only for the player current scope of execution to the initially
     * defined title. Must be used after [.updateTitleForPlayer] to take effect.
     *
     * *** This API is experimental and is not subject to the general compatibility guarantees such
     * API may be changed or may be removed completely in any further release. ***
     *
     * @param player The player to reset the title.
     */
    @Experimental fun resetTitleForPlayer(player: Player)
}
