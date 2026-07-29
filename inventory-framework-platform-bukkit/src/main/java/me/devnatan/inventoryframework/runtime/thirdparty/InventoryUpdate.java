/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 Matsubara
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package me.devnatan.inventoryframework.runtime.thirdparty;

import static me.devnatan.inventoryframework.runtime.thirdparty.ReflectionUtils.*;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;

/**
 * A utility class for update the inventory of a player. This is useful to change the title of an
 * inventory.
 */
@SuppressWarnings({"ConstantConditions", "CallToPrintStackTrace"})
public final class InventoryUpdate {

    private static final Class<?> CHAT_MESSAGE;
    private static final Class<?> PACKET_PLAY_OUT_OPEN_WINDOW;
    private static final Class<?> I_CHAT_BASE_COMPONENT;
    public static final Class<?> CONTAINER;
    private static final Class<?> CONTAINERS;
    private static final Class<?> I_CHAT_MUTABLE_COMPONENT;
    private static final Class<?> MINECRAFT_MENU_TYPE;

    // Methods
    public static final MethodHandle getBukkitView;
    private static final MethodHandle literal;
    private static final MethodHandle asVanilla;

    // Constructors
    private static final MethodHandle chatMessage;
    public static final MethodHandle packetPlayOutOpenWindow;

    // Fields
    public static final MethodHandle activeContainer;
    public static final MethodHandle windowId;

    // Methods factory
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static final Set<String> UNOPENABLES = Sets.newHashSet("CRAFTING", "CREATIVE", "PLAYER");
    private static final boolean SUPPORTS_19 = McVersion.supports(19);
    public static final Object[] DUMMY_COLOR_MODIFIERS = new Object[0];

    static {
        final boolean modern = McVersion.isModern();

        // Initialize classes.
        CHAT_MESSAGE = SUPPORTS_19 ? null : ReflectionUtils.getNMSClass("network.chat", "ChatMessage");
        PACKET_PLAY_OUT_OPEN_WINDOW = modern
                ? ReflectionUtils.getNMSClass("network.protocol.game", "ClientboundOpenScreenPacket")
                : ReflectionUtils.getNMSClass("network.protocol.game", "PacketPlayOutOpenWindow");
        I_CHAT_BASE_COMPONENT = modern
                ? ReflectionUtils.getNMSClass("network.chat", "Component")
                : ReflectionUtils.getNMSClass("network.chat", "IChatBaseComponent");
        CONTAINER = modern
                ? ReflectionUtils.getNMSClass("world.inventory", "AbstractContainerMenu")
                : ReflectionUtils.getNMSClass("world.inventory", "Container");
        // Check if we use containers, otherwise, can throw errors on older versions.
        CONTAINERS = useContainers()
                ? (modern
                        ? ReflectionUtils.getNMSClass("world.inventory", "MenuType")
                        : ReflectionUtils.getNMSClass("world.inventory", "Containers"))
                : null;
        I_CHAT_MUTABLE_COMPONENT = SUPPORTS_19
                ? (modern
                        ? ReflectionUtils.getNMSClass("network.chat", "MutableComponent")
                        : ReflectionUtils.getNMSClass("network.chat", "IChatMutableComponent"))
                : null;
        MINECRAFT_MENU_TYPE = getClassOrNull("net.minecraft.world.inventory.MenuType");

        // Initialize methods.
        getBukkitView = getMethod(CONTAINER, "getBukkitView", MethodType.methodType(InventoryView.class));
        literal = SUPPORTS_19
                ? getMethod(
                        I_CHAT_BASE_COMPONENT,
                        modern ? "literal" : "b",
                        MethodType.methodType(I_CHAT_MUTABLE_COMPONENT, String.class),
                        true)
                : null;

        final Class<?> paperAdventure = getClassOrNull("io.papermc.paper.adventure.PaperAdventure");
        asVanilla = paperAdventure == null
                ? null
                : getMethod(
                        paperAdventure,
                        "asVanilla",
                        MethodType.methodType(
                                getClassOrNull("net.minecraft.network.chat.Component"),
                                getClassOrNull("net.kyori.adventure.text.Component")),
                        true);

        // Initialize constructors.
        chatMessage = SUPPORTS_19 ? null : getConstructor(CHAT_MESSAGE, String.class, Object[].class);
        packetPlayOutOpenWindow = (useContainers())
                ? getConstructor(PACKET_PLAY_OUT_OPEN_WINDOW, int.class, CONTAINERS, I_CHAT_BASE_COMPONENT)
                :
                // Older versions use String instead of Containers, and require an int for the inventory size.
                getConstructor(PACKET_PLAY_OUT_OPEN_WINDOW, int.class, String.class, I_CHAT_BASE_COMPONENT, int.class);

        // Initialize fields.
        activeContainer = modern
                ? getField(ENTITY_PLAYER, CONTAINER, "containerMenu")
                : getField(ENTITY_PLAYER, CONTAINER, "activeContainer", "bR", "bV", "bW", "bU", "bP", "containerMenu");
        windowId = getField(CONTAINER, int.class, modern ? "containerId" : "windowId", "j", "containerId");
    }

    private static Class<?> getClassOrNull(String className) {
        try {
            return Class.forName(className);
        } catch (final ClassNotFoundException ignored) {
        }
        return null;
    }

    /**
     * Update the player inventory, so you can change the title.
     *
     * @param player   whose inventory will be updated.
     * @param newTitle the new title for the inventory.
     */
    @SuppressWarnings("UnstableApiUsage")
    public static void updateInventory(Player player, Object newTitle) {
        Preconditions.checkArgument(player != null, "Cannot update inventory to null player.");

        if (newTitle == null) newTitle = "";

        try {
            if (newTitle instanceof String && ((String) newTitle).length() > 32) {
                newTitle = ((String) newTitle).substring(0, 32);
            }

            if (newTitle instanceof String && McVersion.supports(20)) {
                InventoryView open = player.getOpenInventory();
                if (UNOPENABLES.contains(open.getType().name())) return;
                open.setTitle((String) newTitle);
                return;
            }

            // Get EntityPlayer from CraftPlayer.
            Object craftPlayer = CRAFT_PLAYER.cast(player);
            Object entityPlayer = GET_HANDLE.invoke(craftPlayer);

            // Get activeContainer from EntityPlayer.
            Object activeContainer = getActiveContainer(entityPlayer);

            // Get windowId from activeContainer.
            Integer windowId = (Integer) InventoryUpdate.windowId.invoke(activeContainer);

            // Get InventoryView from activeContainer.
            Object bukkitView = getBukkitView.invoke(activeContainer);
            if (!(bukkitView instanceof InventoryView)) return;

            // Avoiding pattern variable, since some people may be using an older version of java.
            InventoryView view = (InventoryView) bukkitView;
            InventoryType type = view.getTopInventory().getType();

            // Workbenchs and anvils can change their title since 1.14.
            if ((type == InventoryType.WORKBENCH || type == InventoryType.ANVIL) && !useContainers()) return;

            // You can't reopen crafting, creative and player inventory.
            if (UNOPENABLES.contains(type.name())) return;

            int size = view.getTopInventory().getSize();

            // Get container, check is not null.
            Containers container = Containers.getType(type, size);
            if (container == null) return;

            // If the container was added in a newer version than the current, return.
            // Only applies on 1.x; on the year-based scheme (26+) MINOR_NUMBER is the release's own
            // minor (e.g. 1 for 26.1.x) and is unrelated to the container-added-in-1.x versions below,
            // so all containers are considered valid there.
            if (McVersion.current().getMajor() == 1
                    && container.getContainerVersion() > ReflectionUtils.MINOR_NUMBER
                    && useContainers()) {
                return;
            }

            Object packet;
            if (!(newTitle instanceof String)) {
                final Object minecraftComponent = asVanilla.invoke(newTitle);

                // Bukkit uses uppercase "X", Minecraft uses lowercase "x"
                // Bukkit: GENERIC_9X3 / Minecraft: GENERIC_9x3
                final String minecraftEnumName = container.name().replace("X", "x");

                final Object menuType =
                        MINECRAFT_MENU_TYPE.getField(minecraftEnumName).get(null);

                packet = packetPlayOutOpenWindow.invoke(windowId, menuType, minecraftComponent);
            } else {
                // Create new title.
                Object title = createTitleComponent(newTitle);
                Object object = getContainerOrName(container, type);

                packet = useContainers()
                        ? packetPlayOutOpenWindow.invoke(windowId, object, title)
                        : packetPlayOutOpenWindow.invoke(windowId, object, title, size);
            }

            // Send packet sync.
            ReflectionUtils.sendPacketSync(player, packet);

            // Update inventory.
            player.updateInventory();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public static Object getContainerOrName(Containers container, InventoryType type) {
        // Dispensers and droppers use the same container, but in previous versions, use a diferrent minecraft name.
        if (!useContainers() && container == Containers.GENERIC_3X3) {
            return "minecraft:" + type.name().toLowerCase();
        } else {
            return container.getObject();
        }
    }

    public static Object createTitleComponent(Object text) throws Throwable {
        if (McVersion.supports(19)) {
            return literal.invoke(text);
        } else {
            return chatMessage.invoke(text, DUMMY_COLOR_MODIFIERS);
        }
    }

    public static Object getActiveContainer(Object entityPlayer) throws Throwable {
        return InventoryUpdate.activeContainer.invoke(entityPlayer);
    }

    public static MethodHandle getField(Class<?> refc, Class<?> instc, String name, String... extraNames) {
        MethodHandle handle = getFieldHandle(refc, instc, name);
        if (handle != null) return handle;

        if (extraNames != null && extraNames.length > 0) {
            if (extraNames.length == 1) return getField(refc, instc, extraNames[0]);
            return getField(refc, instc, extraNames[0], removeFirst(extraNames));
        }

        return null;
    }

    public static MethodHandle setField(Class<?> refc, Class<?> instc, String name, String... extraNames) {
        MethodHandle handle = setFieldHandle(refc, instc, name);
        if (handle != null) return handle;

        if (extraNames != null && extraNames.length > 0) {
            if (extraNames.length == 1) return setField(refc, instc, extraNames[0]);
            return setField(refc, instc, extraNames[0], removeFirst(extraNames));
        }

        return null;
    }

    private static String[] removeFirst(String[] array) {
        int length = array.length;

        String[] result = new String[length - 1];
        System.arraycopy(array, 1, result, 0, length - 1);

        return result;
    }

    public static MethodHandle getFieldHandle(Class<?> refc, Class<?> inscofc, String name) {
        try {
            for (Field field : refc.getFields()) {
                field.setAccessible(true);

                if (!field.getName().equalsIgnoreCase(name)) continue;

                if (field.getType().isInstance(inscofc) || field.getType().isAssignableFrom(inscofc)) {
                    return LOOKUP.unreflectGetter(field);
                }
            }
            return null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    public static MethodHandle setFieldHandle(Class<?> refc, Class<?> inscofc, String name) {
        try {
            for (Field field : refc.getFields()) {
                field.setAccessible(true);

                if (!field.getName().equalsIgnoreCase(name)) continue;

                if (field.getType().isInstance(inscofc) || field.getType().isAssignableFrom(inscofc)) {
                    return LOOKUP.unreflectSetter(field);
                }
            }
            return null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    public static MethodHandle getConstructor(Class<?> refc, Class<?>... types) {
        try {
            Constructor<?> constructor = refc.getDeclaredConstructor(types);
            constructor.setAccessible(true);
            return LOOKUP.unreflectConstructor(constructor);
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public static MethodHandle getMethod(Class<?> refc, String name, MethodType type) {
        return getMethod(refc, name, type, false);
    }

    public static MethodHandle getMethod(Class<?> refc, String name, MethodType type, boolean isStatic) {
        try {
            if (isStatic) return LOOKUP.findStatic(refc, name, type);
            return LOOKUP.findVirtual(refc, name, type);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    public static MethodHandle getMethod(
            Class<?> refc, String name, MethodType type, boolean isStatic, String... extraNames) {
        MethodHandle handle = getMethod(refc, name, type, isStatic);
        if (handle != null) return handle;

        if (extraNames != null && extraNames.length > 0) {
            if (extraNames.length == 1) return getMethod(refc, extraNames[0], type, isStatic);
            return getMethod(refc, extraNames[0], type, isStatic, removeFirst(extraNames));
        }

        return null;
    }

    /**
     * Containers were added in 1.14, a String were used in previous versions.
     *
     * @return whether to use containers.
     */
    public static boolean useContainers() {
        return McVersion.isModern() || ReflectionUtils.MINOR_NUMBER > 13;
    }

    /**
     * An enum class for the necessaries containers.
     */
    public enum Containers {
        GENERIC_9X1(14, "minecraft:chest", "CHEST"),
        GENERIC_9X2(14, "minecraft:chest", "CHEST"),
        GENERIC_9X3(14, "minecraft:chest", "CHEST", "ENDER_CHEST", "BARREL"),
        GENERIC_9X4(14, "minecraft:chest", "CHEST"),
        GENERIC_9X5(14, "minecraft:chest", "CHEST"),
        GENERIC_9X6(14, "minecraft:chest", "CHEST"),
        GENERIC_3X3(14, null, "DISPENSER", "DROPPER"),
        ANVIL(14, "minecraft:anvil", "ANVIL"),
        BEACON(14, "minecraft:beacon", "BEACON"),
        BREWING_STAND(14, "minecraft:brewing_stand", "BREWING"),
        ENCHANTMENT(14, "minecraft:enchanting_table", "ENCHANTING"),
        FURNACE(14, "minecraft:furnace", "FURNACE"),
        HOPPER(14, "minecraft:hopper", "HOPPER"),
        MERCHANT(14, "minecraft:villager", "MERCHANT"),
        // For an unknown reason, when updating a shulker box, the size of the inventory get a little bigger.
        SHULKER_BOX(14, "minecraft:blue_shulker_box", "SHULKER_BOX"),

        // Added in 1.14, so only works with containers.
        BLAST_FURNACE(14, null, "BLAST_FURNACE"),
        CRAFTING(14, null, "WORKBENCH"),
        GRINDSTONE(14, null, "GRINDSTONE"),
        LECTERN(14, null, "LECTERN"),
        LOOM(14, null, "LOOM"),
        SMOKER(14, null, "SMOKER"),
        // CARTOGRAPHY in 1.14, CARTOGRAPHY_TABLE in 1.15 & 1.16 (container), handle in getObject().
        CARTOGRAPHY_TABLE(14, null, "CARTOGRAPHY"),
        STONECUTTER(14, null, "STONECUTTER"),

        // Added in 1.14, functional since 1.16.
        SMITHING(16, null, "SMITHING");

        private final int containerVersion;
        private final String minecraftName;
        private final String[] inventoryTypesNames;

        private static final char[] alphabet = "abcdefghijklmnopqrstuvwxyz".toCharArray();

        Containers(int containerVersion, String minecraftName, String... inventoryTypesNames) {
            this.containerVersion = containerVersion;
            this.minecraftName = minecraftName;
            this.inventoryTypesNames = inventoryTypesNames;
        }

        /**
         * Get the container based on the current open inventory of the player.
         *
         * @param type type of inventory.
         * @return the container.
         */
        public static Containers getType(InventoryType type, int size) {
            if (type == InventoryType.CHEST) {
                return Containers.valueOf("GENERIC_9X" + size / 9);
            }
            for (Containers container : Containers.values()) {
                for (String bukkitName : container.getInventoryTypesNames()) {
                    if (bukkitName.equalsIgnoreCase(type.toString())) {
                        return container;
                    }
                }
            }
            return null;
        }

        /**
         * Get the object from the container enum.
         *
         * @return a Containers object if 1.14+, otherwise, a String.
         */
        public Object getObject() {
            try {
                if (!useContainers()) return getMinecraftName();
                int version = ReflectionUtils.MINOR_NUMBER;
                String name = (version == 14 && this == CARTOGRAPHY_TABLE) ? "CARTOGRAPHY" : name();
                // Since 1.17, containers go from "a" to "x".
                if (version > 16 && version <= 20) name = String.valueOf(alphabet[ordinal()]);
                // Modern (26+): MenuType fields use lowercase 'x', e.g. GENERIC_9x3.
                if (McVersion.isModern()) name = name.replace("X", "x");

                Field field = CONTAINERS.getField(name);
                return field.get(null);

            } catch (ReflectiveOperationException exception) {
                exception.printStackTrace();
            }
            return null;
        }

        /**
         * Get the version in which the inventory container was added.
         *
         * @return the version.
         */
        public int getContainerVersion() {
            return containerVersion;
        }

        /**
         * Get the name of the inventory from Minecraft for older versions.
         *
         * @return name of the inventory.
         */
        public String getMinecraftName() {
            return minecraftName;
        }

        /**
         * Get inventory types names of the inventory.
         *
         * @return bukkit names.
         */
        public String[] getInventoryTypesNames() {
            return inventoryTypesNames;
        }
    }
}
