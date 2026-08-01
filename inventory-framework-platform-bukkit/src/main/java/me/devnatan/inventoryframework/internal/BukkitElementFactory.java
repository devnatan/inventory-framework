package me.devnatan.inventoryframework.internal;

import static me.devnatan.inventoryframework.runtime.util.InventoryUtils.checkInventoryTypeSupport;

import com.tcoded.folialib.FoliaLib;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import me.devnatan.inventoryframework.*;
import me.devnatan.inventoryframework.bukkit.View;
import me.devnatan.inventoryframework.bukkit.context.*;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.component.Component;
import me.devnatan.inventoryframework.component.ComponentBuilder;
import me.devnatan.inventoryframework.context.*;
import me.devnatan.inventoryframework.logging.Logger;
import me.devnatan.inventoryframework.logging.NoopLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BukkitElementFactory extends ElementFactory {

    private static final ViewType defaultType = ViewType.CHEST;
    private Boolean worksInCurrentPlatform = null;

    private final FoliaLib foliaLib;

    public BukkitElementFactory(Plugin plugin) {
        this.foliaLib = new FoliaLib(plugin);
    }

    @Override
    public @NotNull RootView createUninitializedRoot() {
        return new View();
    }

    @Override
    public @NotNull ViewContainer createContainer(@NotNull IFContext context) {
        final ViewConfig config = context.getConfig();
        final ViewType finalType = config.getType() == null ? defaultType : config.getType();
        checkInventoryTypeSupport(finalType);

        final int size = finalType.normalize(config.getSize());
        if (size != 0 && !finalType.isExtendable() && context.getConfig().getLayout() == null)
            throw new IllegalArgumentException(String.format(
                    "Only \"%s\" type can have a custom size,"
                            + " \"%s\" always have a size of %d. Remove the parameter that specifies the size"
                            + " of the container on %s or just set the type explicitly.",
                    ViewType.CHEST.getIdentifier(),
                    finalType.getIdentifier(),
                    finalType.getMaxSize(),
                    context.getRoot().getClass().getName()));

        final InventoryHolder holder = context instanceof InventoryHolder ? (InventoryHolder) context : null;
        final Inventory inventory =
                InventoryFactory.current().createInventory(holder, finalType, size, config.getTitle());

        return new BukkitViewContainer(inventory, false, finalType, false);
    }

    @Override
    public @NotNull Viewer createViewer(@NotNull Object entity, IFRenderContext context) {
        if (!(entity instanceof Player))
            throw new IllegalArgumentException("createViewer(...) first parameter must be a Player");

        return new BukkitViewer((Player) entity, context);
    }

    @Override
    public IFOpenContext createOpenContext(
            @NotNull RootView root, @Nullable Viewer subject, @NotNull List<Viewer> viewers, Object initialData) {
        return new OpenContext(
                (View) root,
                subject,
                viewers.stream().collect(Collectors.toMap(Viewer::getId, Function.identity())),
                initialData);
    }

    @Override
    public IFRenderContext createRenderContext(
            @NotNull UUID id,
            @NotNull RootView root,
            @NotNull ViewConfig config,
            ViewContainer container,
            @NotNull Map<String, Viewer> viewers,
            Viewer subject,
            Object initialData) {
        return new RenderContext(id, (View) root, config, container, viewers, subject, initialData);
    }

    @Override
    public IFSlotClickContext createSlotClickContext(
            int slotClicked,
            @NotNull Viewer whoClicked,
            @NotNull ViewContainer interactionContainer,
            @Nullable Component componentClicked,
            @NotNull Object origin,
            boolean combined) {
        final IFRenderContext context = whoClicked.getActiveContext();
        return new SlotClickContext(
                slotClicked,
                context,
                whoClicked,
                interactionContainer,
                componentClicked,
                (InventoryClickEvent) origin,
                combined);
    }

    @Override
    public IFSlotRenderContext createSlotRenderContext(
            int slot, @NotNull IFRenderContext parent, @Nullable Viewer viewer, Component component) {
        return new SlotRenderContext(slot, parent, viewer, component);
    }

    @Override
    public IFCloseContext createCloseContext(
            @NotNull Viewer viewer, @NotNull IFRenderContext parent, @NotNull Object closeOrigin) {
        return new CloseContext(viewer, parent, (InventoryCloseEvent) closeOrigin);
    }

    @Override
    public ComponentBuilder<?, Context> createComponentBuilder(@NotNull VirtualView root) {
        return new BukkitItemComponentBuilder(root);
    }

    @Override
    public synchronized boolean worksInCurrentPlatform() {
        if (worksInCurrentPlatform != null) return worksInCurrentPlatform;

        if (foliaLib.isFolia()) return true;

        try {
            Class.forName("org.bukkit.Bukkit");
            worksInCurrentPlatform = true;
        } catch (ClassNotFoundException ignored) {
            // suppress ClassNotFoundException because it will be thrown in PlatformUtils
            worksInCurrentPlatform = false;
        }

        return worksInCurrentPlatform;
    }

    @Override
    public Logger getLogger() {
        return new NoopLogger();
    }

    @Override
    public Job scheduleJobInterval(@NotNull RootView root, long intervalInTicks, @NotNull Runnable execution) {
        return new BukkitTaskJobImpl(foliaLib.getScheduler(), intervalInTicks, execution);
    }
}
