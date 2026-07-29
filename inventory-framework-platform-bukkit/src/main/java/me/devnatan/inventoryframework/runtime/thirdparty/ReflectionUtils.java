/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 Crypto Morin
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

import static me.devnatan.inventoryframework.runtime.thirdparty.McVersion.supports;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * <b>ReflectionUtils</b> - Reflection handler for NMS and CraftBukkit.<br>
 * Caches the packet related methods and is asynchronous.
 * <p>
 * This class does not handle null checks as most of the requests are from the
 * other utility classes that already handle null checks.
 * <p>
 * <a href="https://wiki.vg/Protocol">Clientbound Packets</a> are considered fake
 * updates to the client without changing the actual data. Since all the data is handled
 * by the server.
 * <p>
 * A useful resource used to compare mappings is <a href="https://minidigger.github.io/MiniMappingViewer/#/spigot">Mini's Mapping Viewer</a>
 *
 * @author Crypto Morin
 * @version 7.0.0
 */
public final class ReflectionUtils {
    /**
     * We use reflection mainly to avoid writing a new class for version barrier.
     * The version barrier is for NMS that uses the Minecraft version as the main package name.
     * <p>
     * E.g. EntityPlayer in 1.15 is in the class {@code net.minecraft.server.v1_15_R1}
     * but in 1.14 it's in {@code net.minecraft.server.v1_14_R1}
     * In order to maintain cross-version compatibility we cannot import these classes.
     * <p>
     * Performance is not a concern for these specific statically initialized values.
     * <p>
     * <a href="https://www.spigotmc.org/wiki/spigot-nms-and-minecraft-versions-legacy/">Versions Legacy</a>
     */
    public static final String NMS_VERSION;

    static { // This needs to be right below VERSION because of initialization order.
        // This package loop is used to avoid implementation-dependant strings like Bukkit.getVersion() or
        // Bukkit.getBukkitVersion()
        // which allows easier testing as well.
        String found = null;
        for (Package pack : Package.getPackages()) {
            String name = pack.getName();

            // .v because there are other packages.
            if (name.startsWith("org.bukkit.craftbukkit.v")) {
                found = pack.getName().split("\\.")[3];

                // Just a final guard to make sure it finds this important class.
                // As a protection for forge+bukkit implementation that tend to mix versions.
                // The real CraftPlayer should exist in the package.
                // Note: Doesn't seem to function properly. Will need to separate the version
                // handler for NMS and CraftBukkit for softwares like catmc.
                try {
                    Class.forName("org.bukkit.craftbukkit." + found + ".entity.CraftPlayer");
                    break;
                } catch (ClassNotFoundException e) {
                    found = null;
                }
            }
        }
        if (found == null) {
            try {
                Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer");
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(
                        "Failed to parse server version. Could not find any package starting with name: 'org.bukkit.craftbukkit.v'");
            }
        }
        NMS_VERSION = found;
    }

    /**
     * The raw minor version number.
     * E.g. {@code v1_17_R1} to {@code 17}
     *
     * @see McVersion#supports(int)
     * @since 4.0.0
     */
    public static final int MINOR_NUMBER = McVersion.current().getMinor();
    /**
     * The raw patch version number.
     * E.g. {@code v1_17_R1} to {@code 1}
     * <p>
     * I'd not recommend developers to support individual patches at all. You should always support the latest patch.
     * For example, between v1.14.0, v1.14.1, v1.14.2, v1.14.3 and v1.14.4 you should only support v1.14.4
     * <p>
     * This can be used to warn server owners when your plugin will break on older patches.
     *
     * @since 7.0.0
     */
    public static final int PATCH_NUMBER = McVersion.current().getPatch();

    /**
     * Gets the full version information of the server. Useful for including in errors.
     *
     * @since 7.0.0
     */
    public static String getVersionInformation() {
        return "(NMS: " + NMS_VERSION + " | " + "Minecraft: "
                + Bukkit.getVersion() + " | " + "Bukkit: "
                + Bukkit.getBukkitVersion() + ')';
    }

    /**
     * Mojang remapped their NMS in 1.17: <a href="https://www.spigotmc.org/threads/spigot-bungeecord-1-17.510208/#post-4184317">Spigot Thread</a>
     */
    public static final String CRAFTBUKKIT_PACKAGE =
            NMS_VERSION != null ? "org.bukkit.craftbukkit." + NMS_VERSION + '.' : "org.bukkit.craftbukkit.";

    public static final String NMS_PACKAGE =
            v(17, "net.minecraft.").orElse("net.minecraft.server." + NMS_VERSION + '.');
    /**
     * A nullable public accessible field only available in {@code EntityPlayer}.
     * This can be null if the player is offline.
     */
    private static final MethodHandle PLAYER_CONNECTION;
    /**
     * Responsible for getting the NMS handler {@code EntityPlayer} object for the player.
     * {@code CraftPlayer} is simply a wrapper for {@code EntityPlayer}.
     * Used mainly for handling packet related operations.
     * <p>
     * This is also where the famous player {@code ping} field comes from!
     */
    public static final MethodHandle GET_HANDLE;
    /**
     * Sends a packet to the player's client through a {@code NetworkManager} which
     * is where {@code ProtocolLib} controls packets by injecting channels!
     */
    private static final MethodHandle SEND_PACKET;

    /**
     * The class for the NMS CraftPlayer.
     */
    public static final Class<?> CRAFT_PLAYER;

    /**
     * The class for the NMS EntityPlayer.
     *
     * @see net.minecraft.server.level.ServerPlayer
     */
    public static final Class<?> ENTITY_PLAYER;

    static {
        final boolean modern = McVersion.isModern();
        ENTITY_PLAYER = getNMSClass("server.level", modern ? "ServerPlayer" : "EntityPlayer");
        CRAFT_PLAYER = getCraftClass("entity.CraftPlayer");
        Class<?> playerConnection =
                getNMSClass("server.network", modern ? "ServerGamePacketListenerImpl" : "PlayerConnection");

        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle sendPacket = null, getHandle = null, connection = null;

        try {
            String connectionFieldName = modern
                    ? "connection"
                    : v(21, 3, "f").v(20, "c").v(17, "b").v(21, 6, "g").orElse("playerConnection");
            connection = lookup.findGetter(ENTITY_PLAYER, connectionFieldName, playerConnection);

            getHandle = lookup.findVirtual(CRAFT_PLAYER, "getHandle", MethodType.methodType(ENTITY_PLAYER));

            final VersionHandler<String> methodVersion =
                    v(21, "send").v(20, "b").v(18, "a");

            String sendMethodName = modern ? "send" : methodVersion.orElse("sendPacket");
            sendPacket = lookup.findVirtual(
                    playerConnection,
                    sendMethodName,
                    MethodType.methodType(void.class, getNMSClass("network.protocol", "Packet")));
        } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException ex) {
            ex.printStackTrace();
        }

        PLAYER_CONNECTION = connection;
        SEND_PACKET = sendPacket;
        GET_HANDLE = getHandle;
    }

    public static boolean supportsMC1202() {
        return supports(20) && PATCH_NUMBER >= 2;
    }

    private ReflectionUtils() {}

    /**
     * This method is purely for readability.
     * No performance is gained.
     *
     * @since 3.3.0
     */
    public static <T> VersionHandler<T> v(int minor, int patch, T handle) {
        return new VersionHandler<>(minor, patch, handle);
    }

    /**
     * This method is purely for readability.
     * No performance is gained.
     *
     * @since 5.0.0
     */
    public static <T> VersionHandler<T> v(int version, T handle) {
        return new VersionHandler<>(version, 0, handle);
    }

    /**
     * Get a NMS (net.minecraft.server) class which accepts a package for 1.17 compatibility.
     *
     * @param newPackage the 1.17 package name.
     * @param name       the name of the class.
     * @return the NMS class or null if not found.
     * @since 4.0.0
     */
    @Nullable
    public static Class<?> getNMSClass(String newPackage, String name) {
        if (supports(17)) name = newPackage + '.' + name;
        return getNMSClass(name);
    }

    /**
     * Get a NMS (net.minecraft.server) class.
     *
     * @param name the name of the class.
     * @return the NMS class or null if not found.
     * @since 1.0.0
     */
    @Nullable
    public static Class<?> getNMSClass(String name) {
        try {
            return Class.forName(NMS_PACKAGE + name);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static Object getEntityPlayer(Object craftPlayer) throws Throwable {
        return GET_HANDLE.invoke(craftPlayer);
    }

    /**
     * Sends a packet to the player synchronously if they're online.
     *
     * @param player  the player to send the packet to.
     * @param packets the packets to send.
     * @since 2.0.0
     */
    public static void sendPacketSync(Player player, Object... packets) throws Throwable {
        Object entityPlayer = getEntityPlayer(player);
        Object connection = PLAYER_CONNECTION.invoke(entityPlayer);

        // Checking if the connection is not null is enough. There is no need to check if the player is online.
        if (connection != null) {
            for (Object packet : packets) SEND_PACKET.invoke(connection, packet);
        }
    }

    /**
     * Get a CraftBukkit (org.bukkit.craftbukkit) class.
     *
     * @param name the name of the class to load.
     * @return the CraftBukkit class or null if not found.
     * @since 1.0.0
     */
    @Nullable
    public static Class<?> getCraftClass(String name) {
        try {
            return Class.forName(CRAFTBUKKIT_PACKAGE + name);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static final class VersionHandler<T> {
        private int minor;
        private int patch;
        private T handle;

        private VersionHandler(int minor, int patch, T handle) {
            if (supports(minor)) {
                this.minor = minor;
                this.patch = patch;
                this.handle = handle;
            }
        }

        public VersionHandler<T> v(int version, T handle) {
            return v(minor, version, handle);
        }

        public VersionHandler<T> v(int minor, int patch, T handle) {
            if (minor == this.minor && patch == this.patch)
                throw new IllegalArgumentException(
                        "Cannot have duplicate version handles for version: 1." + minor + '.' + patch);
            if (((minor == this.minor && patch > this.patch) || minor > this.minor) && supports(minor, patch)) {
                this.minor = minor;
                this.patch = patch;
                this.handle = handle;
            }
            return this;
        }

        public T orElse(T handle) {
            return this.minor == 0 ? handle : this.handle;
        }
    }
}
