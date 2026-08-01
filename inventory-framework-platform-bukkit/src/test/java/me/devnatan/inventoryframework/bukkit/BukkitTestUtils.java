package me.devnatan.inventoryframework.bukkit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BukkitTestUtils {

    public static Player createPlayerMock() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(new UUID(0, 1));
        return player;
    }

    public static ViewFrame createViewFrameMock() {
        return ViewFrame.create(mock(Plugin.class));
    }
}
