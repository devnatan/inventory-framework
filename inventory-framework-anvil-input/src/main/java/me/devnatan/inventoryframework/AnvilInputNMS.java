package me.devnatan.inventoryframework;

import static me.devnatan.inventoryframework.runtime.thirdparty.InventoryUpdate.CONTAINER;
import static me.devnatan.inventoryframework.runtime.thirdparty.InventoryUpdate.createTitleComponent;
import static me.devnatan.inventoryframework.runtime.thirdparty.InventoryUpdate.getConstructor;
import static me.devnatan.inventoryframework.runtime.thirdparty.InventoryUpdate.getContainerOrName;
import static me.devnatan.inventoryframework.runtime.thirdparty.InventoryUpdate.getField;
import static me.devnatan.inventoryframework.runtime.thirdparty.InventoryUpdate.getMethod;
import static me.devnatan.inventoryframework.runtime.thirdparty.InventoryUpdate.packetPlayOutOpenWindow;
import static me.devnatan.inventoryframework.runtime.thirdparty.InventoryUpdate.setField;
import static me.devnatan.inventoryframework.runtime.thirdparty.InventoryUpdate.setFieldHandle;
import static me.devnatan.inventoryframework.runtime.thirdparty.InventoryUpdate.useContainers;
import static me.devnatan.inventoryframework.runtime.thirdparty.ReflectionUtils.ENTITY_PLAYER;
import static me.devnatan.inventoryframework.runtime.thirdparty.ReflectionUtils.getNMSClass;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.Objects;
import me.devnatan.inventoryframework.runtime.thirdparty.InventoryUpdate;
import me.devnatan.inventoryframework.runtime.thirdparty.McVersion;
import me.devnatan.inventoryframework.runtime.thirdparty.ReflectionUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

class AnvilInputNMS {

    // CONSTRUCTORS
    private static final MethodHandle ANVIL_CONSTRUCTOR;
    private static final Class<?> ANVIL;

    // METHODS
    private static final MethodHandle GET_PLAYER_NEXT_CONTAINER_COUNTER;
    private static final MethodHandle GET_PLAYER_INVENTORY;
    private static final MethodHandle SET_PLAYER_ACTIVE_CONTAINER;
    private static final MethodHandle ADD_CONTAINER_SLOT_LISTENER;
    private static final MethodHandle INIT_MENU;
    private static final MethodHandle GET_TOP_INVENTORY;

    // FIELDS
    private static final MethodHandle CONTAINER_CHECK_REACHABLE;
    private static final MethodHandle PLAYER_DEFAULT_CONTAINER;
    private static final MethodHandle CONTAINER_WINDOW_ID;

    static {
        try {
            final boolean modern = McVersion.isModern();

            ANVIL = Objects.requireNonNull(
                    getNMSClass("world.inventory", modern ? "AnvilMenu" : "ContainerAnvil"),
                    "AnvilMenu/ContainerAnvil NMS class not found");

            final Class<?> playerInventoryClass =
                    getNMSClass("world.entity.player", modern ? "Inventory" : "PlayerInventory");

            ANVIL_CONSTRUCTOR = getConstructor(ANVIL, int.class, playerInventoryClass);
            CONTAINER_CHECK_REACHABLE = setFieldHandle(CONTAINER, boolean.class, "checkReachable");

            final Class<?> containerPlayer =
                    getNMSClass("world.inventory", modern ? "InventoryMenu" : "ContainerPlayer");
            PLAYER_DEFAULT_CONTAINER = getField(ENTITY_PLAYER, containerPlayer, "inventoryMenu", "bQ", "bR");

            final String activeContainerFieldName =
                    modern ? "containerMenu" : (ReflectionUtils.supportsMC1202() ? "bS" : "bR");
            SET_PLAYER_ACTIVE_CONTAINER = setField(
                    ENTITY_PLAYER,
                    containerPlayer,
                    modern ? "containerMenu" : "activeContainer",
                    "containerMenu",
                    activeContainerFieldName);

            GET_PLAYER_NEXT_CONTAINER_COUNTER =
                    getMethod(ENTITY_PLAYER, "nextContainerCounter", MethodType.methodType(int.class));

            GET_PLAYER_INVENTORY = modern
                    ? getMethod(ENTITY_PLAYER, "getInventory", MethodType.methodType(playerInventoryClass))
                    : getMethod(
                            ENTITY_PLAYER,
                            "fN",
                            MethodType.methodType(playerInventoryClass),
                            false,
                            "fR",
                            "getInventory");

            CONTAINER_WINDOW_ID =
                    setField(CONTAINER, int.class, modern ? "containerId" : "windowId", "containerId", "j");

            final Class<?> slotListenerClass =
                    getNMSClass("world.inventory", modern ? "ContainerListener" : "ICrafting");
            ADD_CONTAINER_SLOT_LISTENER = getMethod(
                    CONTAINER, modern ? "addSlotListener" : "a", MethodType.methodType(void.class, slotListenerClass));
            INIT_MENU =
                    getMethod(ENTITY_PLAYER, modern ? "initMenu" : "a", MethodType.methodType(void.class, CONTAINER));

            GET_TOP_INVENTORY =
                    getMethod(InventoryView.class, "getTopInventory", MethodType.methodType(Inventory.class));
        } catch (Exception exception) {
            throw new RuntimeException(
                    "Unsupported version for Anvil Input feature: " + ReflectionUtils.getVersionInformation(),
                    exception);
        }
    }

    private AnvilInputNMS() {}

    public static Inventory open(Player player, Object title, String initialInput) {
        try {
            final Object entityPlayer = ReflectionUtils.getEntityPlayer(player);
            final Object defaultContainer = PLAYER_DEFAULT_CONTAINER.invoke(entityPlayer);
            SET_PLAYER_ACTIVE_CONTAINER.invoke(entityPlayer, defaultContainer);

            final int windowId = (int) GET_PLAYER_NEXT_CONTAINER_COUNTER.invoke(entityPlayer);
            final Object anvilContainer = ANVIL_CONSTRUCTOR.invoke(windowId, GET_PLAYER_INVENTORY.invoke(entityPlayer));
            CONTAINER_CHECK_REACHABLE.invoke(anvilContainer, false);

            final AnvilInventory inventory =
                    (AnvilInventory) GET_TOP_INVENTORY.invoke(InventoryUpdate.getBukkitView.invoke(anvilContainer));

            inventory.setMaximumRepairCost(0);

            @SuppressWarnings("deprecation")
            final ItemStack item = new ItemStack(Material.PAPER, 1, (short) 0);
            final ItemMeta meta = Objects.requireNonNull(item.getItemMeta());
            meta.setDisplayName(initialInput);
            item.setItemMeta(meta);
            inventory.setItem(0, item);

            Object nmsContainers = getContainerOrName(InventoryUpdate.Containers.ANVIL, InventoryType.ANVIL);
            Object updatedTitle = createTitleComponent(title == null ? "" : title);
            Object openWindowPacket = useContainers()
                    ? packetPlayOutOpenWindow.invoke(windowId, nmsContainers, updatedTitle)
                    : packetPlayOutOpenWindow.invoke(
                            windowId, nmsContainers, updatedTitle, InventoryType.ANVIL.getDefaultSize());

            ReflectionUtils.sendPacketSync(player, openWindowPacket);
            SET_PLAYER_ACTIVE_CONTAINER.invoke(entityPlayer, anvilContainer);
            CONTAINER_WINDOW_ID.invoke(anvilContainer, windowId);

            if (McVersion.supports(19)) {
                INIT_MENU.invoke(entityPlayer, anvilContainer);
            } else {
                ADD_CONTAINER_SLOT_LISTENER.invoke(anvilContainer, player);
            }
            return inventory;
        } catch (Throwable throwable) {
            throw new RuntimeException("Something went wrong while opening Anvil Input NMS inventory.", throwable);
        }
    }
}
