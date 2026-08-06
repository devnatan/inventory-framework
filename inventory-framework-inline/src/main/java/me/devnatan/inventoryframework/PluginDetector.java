package me.devnatan.inventoryframework;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Best-effort detection of the plugin that is calling into the inline builder API, so that users
 * don't have to explicitly pass their plugin instance around.
 * <p>
 * Walks the current call stack looking for the first frame that doesn't belong to this library
 * (identified by classloader identity rather than package name, since this library is commonly
 * shaded and relocated) and resolves it to a {@link Plugin} via {@link JavaPlugin#getProvidingPlugin(Class)}.
 */
final class PluginDetector {

    private PluginDetector() {}

    static Plugin detectCallingPlugin() {
        final ClassLoader ownLoader = PluginDetector.class.getClassLoader();
        final StackTraceElement[] stack = Thread.currentThread().getStackTrace();

        for (final StackTraceElement element : stack) {
            final Class<?> frameClass;
            try {
                frameClass = Class.forName(element.getClassName(), false, ownLoader);
            } catch (final Throwable ignored) {
                continue;
            }

            if (frameClass.getClassLoader() == ownLoader) continue;

            try {
				return JavaPlugin.getProvidingPlugin(frameClass);
            } catch (final Throwable ignored) {
                // frame doesn't belong to a plugin class loader, keep looking up the stack
            }
        }

        throw new IllegalStateException("Could not automatically determine the plugin that owns this view. "
                + "Set it explicitly with ViewBuilder#plugin(Plugin).");
    }
}
