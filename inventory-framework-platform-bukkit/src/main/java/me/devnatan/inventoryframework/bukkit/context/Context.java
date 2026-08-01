package me.devnatan.inventoryframework.bukkit.context;

import java.util.List;
import me.devnatan.inventoryframework.context.IFConfinedContext;
import me.devnatan.inventoryframework.context.IFOpenContext;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

public interface Context extends IFConfinedContext {

    /**
     * The player for the current interaction context.
     * <p>
     * Contexts can be shared and contain multiple viewers, this method will
     * always return the player for the current event.
     *
     * @return A player in this interaction context.
     */
    @UnknownNullability
    Player getPlayer();

    /**
     * <p><b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     */
    @ApiStatus.Experimental
    List<Player> getAllPlayers();

    /**
     * Updates the container title for a specific player.
     *
     * <p>This should not be used before the container is opened, if you need to set the __initial
     * title__ use {@link IFOpenContext#modifyConfig()} on open handler instead.
     *
     * <p>This method is version dependant, so it may be that your server version is not yet
     * supported, if you try to use this method and fail (can fail silently), report it to the
     * library developers to add support to your version.
     *
     * <p><b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     *
     * @param title  The new container title.
     * @param player The player to update the title.
     */
    @ApiStatus.Experimental
    void updateTitleForPlayer(@NotNull String title, @NotNull Player player);

    /**
     * Resets the container title only for the player current scope of execution to the initially
     * defined title. Must be used after {@link #updateTitleForPlayer(String, Player)} to take effect.
     *
     * <p><b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     *
     * @param player The player to reset the title.
     */
    @ApiStatus.Experimental
    void resetTitleForPlayer(@NotNull Player player);
}
