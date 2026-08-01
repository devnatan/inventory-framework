package me.devnatan.inventoryframework.bukkit;

import static me.devnatan.inventoryframework.TestUtils.createRootMock;
import static me.devnatan.inventoryframework.bukkit.BukkitTestUtils.createPlayerMock;
import static me.devnatan.inventoryframework.bukkit.BukkitTestUtils.createViewFrameMock;
import static org.mockito.Mockito.*;

import me.devnatan.inventoryframework.RootView;
import org.bukkit.entity.Item;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.junit.jupiter.api.Test;

public class IFInventoryListenerTest {

    //    @Test
    //    void shouldCancelItemDrop() {
    //        RootView root = createRootMock();
    //        PlayerDropItemEvent event = mock(PlayerDropItemEvent.class);
    //        ViewFrame viewFrame = mock(ViewFrame.class);
    //        when(viewFrame.getCurrentView(any())).thenReturn(root);
    //
    //        ViewConfig config = mock(ViewConfig.class);
    //        when(config.getOptions()).thenReturn(mock(HashMap.class));
    //        when(config.isOptionSet(eq(CANCEL_ON_DROP))).thenReturn(true);
    //        when(config.getOptionValue(eq(CANCEL_ON_DROP))).thenReturn(true);
    //
    //        IFContext context = createContextMock(root, IFContext.class);
    //        when(context.getConfig()).thenReturn(config);
    //        when(root.getContext(any(Viewer.class))).thenReturn(context);
    //
    //        new IFInventoryListener(viewFrame).onItemDrop(event);
    //        verify(root).getContext(any(Viewer.class));
    //        verify(event).setCancelled(eq(true));
    //    }

    @Test
    void skipItemDropEventIfRootIsNotFound() {
        RootView root = createRootMock();
        PlayerDropItemEvent event = new PlayerDropItemEvent(createPlayerMock(), mock(Item.class));
        ViewFrame viewFrame = createViewFrameMock();
        when(viewFrame.getViewer(createPlayerMock())).thenReturn(null);

        new IFInventoryListener(viewFrame).onItemDrop(event);
        verify(root, never()).getConfig();
    }

    //    @Test
    //    void skipItemDropEventIfConfigIsNotSet() {
    //        RootView root = createRootMock();
    //        PlayerDropItemEvent event = mock(PlayerDropItemEvent.class);
    //        ViewFrame viewFrame = mock(ViewFrame.class);
    //        when(viewFrame.getCurrentView(any())).thenReturn(root);
    //
    //        ViewConfig config = mock(ViewConfig.class);
    //        when(config.getOptions()).thenReturn(mock(HashMap.class));
    //        when(config.isOptionSet(eq(CANCEL_ON_DROP))).thenReturn(false);
    //
    //        IFContext context = createContextMock(root, IFContext.class);
    //        when(context.getConfig()).thenReturn(config);
    //        when(root.getContext(any(Viewer.class))).thenReturn(context);
    //
    //        new IFInventoryListener(viewFrame).onItemDrop(event);
    //        verify(root).getContext(any(Viewer.class));
    //        verify(config, never()).getOptionValue(eq(CANCEL_ON_DROP));
    //        verify(event, never()).setCancelled(eq(true));
    //    }
    //
    //    @Test
    //    void shouldCancelItemPickup() {
    //        RootView root = createRootMock();
    //        PlayerPickupItemEvent event = mock(PlayerPickupItemEvent.class);
    //        ViewFrame viewFrame = mock(ViewFrame.class);
    //        when(viewFrame.getCurrentView(any())).thenReturn(root);
    //
    //        ViewConfig config = mock(ViewConfig.class);
    //        when(config.getOptions()).thenReturn(mock(HashMap.class));
    //        when(config.isOptionSet(eq(CANCEL_ON_PICKUP))).thenReturn(true);
    //        when(config.getOptionValue(eq(CANCEL_ON_PICKUP))).thenReturn(true);
    //
    //        IFContext context = createContextMock(root, IFContext.class);
    //        when(context.getConfig()).thenReturn(config);
    //        when(root.getContext(anyString())).thenReturn(context);
    //
    //        new IFInventoryListener(viewFrame).onItemPickup(event);
    //        verify(root).getContext(anyString());
    //        verify(event).setCancelled(eq(true));
    //    }

    @Test
    void skipItemPickupEventIfRootIsNotFound() {
        PlayerPickupItemEvent event = new PlayerPickupItemEvent(createPlayerMock(), mock(Item.class), 10);
        RootView root = createRootMock();
        ViewFrame viewFrame = createViewFrameMock();
        when(viewFrame.getViewer(createPlayerMock())).thenReturn(null);

        new IFInventoryListener(viewFrame).onItemPickup(event);
        verify(root, never()).getConfig();
    }

    //    @Test
    //    void skipItemPickupEventIfConfigIsNotSet() {
    //        RootView root = createRootMock();
    //        PlayerPickupItemEvent event = mock(PlayerPickupItemEvent.class);
    //        ViewFrame viewFrame = mock(ViewFrame.class);
    //        when(viewFrame.getCurrentView(any())).thenReturn(root);
    //
    //        ViewConfig config = mock(ViewConfig.class);
    //        when(config.getOptions()).thenReturn(mock(HashMap.class));
    //        when(config.isOptionSet(eq(CANCEL_ON_PICKUP))).thenReturn(false);
    //
    //        IFContext context = createContextMock(root, IFContext.class);
    //        when(context.getConfig()).thenReturn(config);
    //        when(root.getContext(anyString())).thenReturn(context);
    //
    //        new IFInventoryListener(viewFrame).onItemPickup(event);
    //        verify(root).getContext(anyString());
    //        verify(config, never()).getOptionValue(eq(CANCEL_ON_PICKUP));
    //        verify(event, never()).setCancelled(eq(true));
    //    }
}
