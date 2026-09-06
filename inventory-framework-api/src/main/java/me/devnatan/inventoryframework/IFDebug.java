package me.devnatan.inventoryframework;

import java.util.function.Supplier;
import me.devnatan.inventoryframework.logging.Logger;
import org.intellij.lang.annotations.PrintFormat;
import org.jetbrains.annotations.ApiStatus;

/**
 * <b><i> This is an internal inventory-framework API that should not be used from outside of
 * this library. No compatibility guarantees are provided. </i></b>
 */
@ApiStatus.Internal
public final class IFDebug {

    private static final String SYSTEM_PROPERTY = "me.devnatan.inventoryframework.debug";

    private static Boolean DEBUG_ENABLED = null;

    private static Logger logger;

    static {
        DEBUG_ENABLED = Boolean.parseBoolean(System.getProperty(SYSTEM_PROPERTY, "false"));
    }

    private IFDebug() {}

    /** Enables InventoryFramework debug. */
    public static void enable(Logger logger) {
        IFDebug.logger = logger;
        DEBUG_ENABLED = true;
        debug("Debug enabled");
    }

    /**
     * Prints a message if debug is enabled.
     *
     * @param message Message to print
     * @param args Arguments to apply to the message
     */
    public static void debug(Supplier<String> message, Object... args) {
        if (!DEBUG_ENABLED) return;
        logger.debug(String.format(message.get(), args));
    }

    /**
     * Prints a message if debug is enabled.
     *
     * @param message Message to print
     * @param args Arguments to apply to the message
     */
    public static void debug(@PrintFormat String message, Object... args) {
        if (!DEBUG_ENABLED) return;
        logger.debug(String.format(message, args));
    }
}
