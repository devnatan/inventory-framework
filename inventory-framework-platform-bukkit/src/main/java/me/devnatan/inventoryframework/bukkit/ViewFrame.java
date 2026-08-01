package me.devnatan.inventoryframework.bukkit;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import me.devnatan.inventoryframework.IFDebug;
import me.devnatan.inventoryframework.IFViewFrame;
import me.devnatan.inventoryframework.PlatformView;
import me.devnatan.inventoryframework.Viewer;
import me.devnatan.inventoryframework.context.EndlessContextInfo;
import me.devnatan.inventoryframework.context.IFContext;
import me.devnatan.inventoryframework.feature.DefaultFeatureInstaller;
import me.devnatan.inventoryframework.feature.Feature;
import me.devnatan.inventoryframework.feature.FeatureInstaller;
import me.devnatan.inventoryframework.internal.BukkitElementFactory;
import me.devnatan.inventoryframework.internal.PlatformUtils;
import me.devnatan.inventoryframework.runtime.thirdparty.Metrics;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public class ViewFrame extends IFViewFrame<ViewFrame, View> {

    private static final String BSTATS_SYSTEM_PROP = "inventory-framework.enable-bstats";
    private static final int BSTATS_PROJECT_ID = 15518;
    private static final String PLUGIN_FQN = "me#devnatan#inventoryframework#runtime#InventoryFramework";

    private static final String RELOCATION_MESSAGE =
            "Inventory Framework is running as a shaded non-relocated library. It's extremely recommended that "
                    + "you relocate the library package. Learn more about on docs: "
                    + "https://github.com/DevNatan/inventory-framework/wiki/Installation#preventing-library-conflicts";

    private final Plugin owner;
    private final FeatureInstaller<ViewFrame> featureInstaller = new DefaultFeatureInstaller<>(this);

    private ViewFrame(Plugin owner) {
        this.owner = owner;
    }

    @NotNull
    public final Plugin getOwner() {
        return owner;
    }

    // region Opening
    /**
     * Opens a view to a player.
     *
     * @param viewClass The target view to be opened.
     * @param player    The player that the view will be open to.
     * @return The id of the newly created {@link IFContext}.
     */
    public final String open(@NotNull Class<? extends View> viewClass, @NotNull Player player) {
        return open(viewClass, player, null);
    }

    /**
     * Opens a view to a player with initial data.
     *
     * @param viewClass   The target view to be opened.
     * @param player      The player that the view will be open to.
     * @param initialData The initial data.
     * @return The id of the newly created {@link IFContext}.
     */
    public final String open(@NotNull Class<? extends View> viewClass, @NotNull Player player, Object initialData) {
        return open(viewClass, Collections.singletonList(player), initialData);
    }

    /**
     * Opens a view to more than one player.
     * <p>
     * These players will see the same inventory and share the same context.
     *
     * <p><b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     *
     * @param viewClass The target view to be opened.
     * @param players   The players that the view will be open to.
     * @return The id of the newly created {@link IFContext}.
     */
    @ApiStatus.Experimental
    public final String open(@NotNull Class<? extends View> viewClass, @NotNull Collection<? extends Player> players) {
        return open(viewClass, players, null);
    }

    /**
     * Opens a view to more than one player with initial data.
     * <p>
     * These players will see the same inventory and share the same context.
     *
     * <p><b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     *
     * @param viewClass   The target view to be opened.
     * @param players     The players that the view will be open to.
     * @param initialData The initial data.
     * @return The id of the newly created {@link IFContext}.
     */
    @ApiStatus.Experimental
    public final String open(
            @NotNull Class<? extends View> viewClass,
            @NotNull Collection<? extends Player> players,
            Object initialData) {
        return internalOpen(
                viewClass,
                players.stream()
                        .collect(Collectors.toMap(player -> player.getUniqueId().toString(), Function.identity())),
                initialData);
    }

    /**
     * Opens an already active context to a player.
     * <p>
     * <b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     *
     * @param contextId The id of the context.
     * @param player Who the context will be open to.
     */
    @ApiStatus.Experimental
    public final void openActive(
            @NotNull Class<? extends View> viewClass, @NotNull String contextId, @NotNull Player player) {
        openActive(viewClass, contextId, player, null);
    }

    /**
     * Opens an already active context to a player.
     * <p>
     * <b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     *
     * @param contextId The id of the context.
     * @param player Who the context will be open to.
     * @param initialData Initial data to pass to {@link PlatformView#onViewerAdded(IFContext, Object, Object)}.
     */
    @ApiStatus.Experimental
    public final void openActive(
            @NotNull Class<? extends View> viewClass,
            @NotNull String contextId,
            @NotNull Player player,
            Object initialData) {
        internalOpenActiveContext(viewClass, contextId, player, initialData);
    }

    /**
     * Opens an already active context to a player.
     * <p>
     * <b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     *
     * @param endlessContextInfo The id of the context.
     * @param player Who the context will be open to.
     */
    @ApiStatus.Experimental
    public final void openEndless(@NotNull EndlessContextInfo endlessContextInfo, @NotNull Player player) {
        openEndless(endlessContextInfo, player, null);
    }

    /**
     * Opens an already active context to a player.
     * <p>
     * <b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     *
     * @param endlessContextInfo The id of the context.
     * @param player Who the context will be open to.
     * @param initialData Initial data to pass to {@link PlatformView#onViewerAdded(IFContext, Object, Object)}.
     */
    @SuppressWarnings("unchecked")
    @ApiStatus.Experimental
    public final void openEndless(
            @NotNull EndlessContextInfo endlessContextInfo, @NotNull Player player, Object initialData) {
        openActive(
                (Class<? extends View>) endlessContextInfo.getView().getClass(),
                endlessContextInfo.getContextId(),
                player,
                initialData);
    }
    // endregion

    @Override
    public final ViewFrame register() {
        if (isRegistered()) throw new IllegalStateException("This view frame is already registered");

        PlatformUtils.setFactory(new BukkitElementFactory(getOwner()));
        tryEnableMetrics();
        checkRelocationIssues();
        setRegistered(true);
        getPipeline().execute(IFViewFrame.FRAME_REGISTERED, this);
        initializeViews();
        getOwner().getServer().getPluginManager().registerEvents(new IFInventoryListener(this), getOwner());
        return this;
    }

    @Override
    public final void unregister() {
        if (!isRegistered()) return;

        // Locks new operations while unregistering
        setRegistered(false);

        final Iterator<View> iterator = registeredViews.values().iterator();
        while (iterator.hasNext()) {
            final View view = iterator.next();
            try {
                view.closeForEveryone();
            } catch (final RuntimeException ignored) {
            }
            iterator.remove();
        }
        getPipeline().execute(IFViewFrame.FRAME_UNREGISTERED, this);
    }

    // region Internals
    private void tryEnableMetrics() {
        final ServicesManager servicesManager = getOwner().getServer().getServicesManager();
        if (servicesManager.isProvidedFor(IFViewFrame.class)) return;

        final boolean metricsEnabled =
                Boolean.parseBoolean(System.getProperty(BSTATS_SYSTEM_PROP, String.valueOf(Boolean.TRUE)));

        if (!metricsEnabled) {
            IFDebug.debug("Metrics disabled");
            return;
        }

        if (!(getOwner() instanceof JavaPlugin)) {
            getOwner()
                    .getLogger()
                    .warning("InventoryFramework BStats metrics cannot be"
                            + " enabled since ViewFrame's owner is not a JavaPlugin.");
            return;
        }

        try {
            new Metrics((JavaPlugin) getOwner(), BSTATS_PROJECT_ID);
        } catch (final Exception exception) {
            IFDebug.debug("Unable to enable metrics: %s", exception.getMessage());
        }
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private void initializeViews() {
        for (final Map.Entry<UUID, View> entry : getRegisteredViews().entrySet()) {
            final View view = entry.getValue();

            try {
                view.internalInitialization(this);
                view.setInitialized(true);
            } catch (final Throwable exception) {
                view.setInitialized(false);
                getOwner()
                        .getLogger()
                        .severe(String.format(
                                "An error occurred while enabling view %s: %s",
                                view.getClass().getName(), exception));
                exception.printStackTrace();
            }
        }
    }

    private void checkRelocationIssues() {
        final Plugin plugin = getOwner();
        final boolean isLibraryAsPluginEnabled =
                getOwner().getServer().getPluginManager().isPluginEnabled("InventoryFramework");
        boolean isLibraryPresent = false;
        try {
            Class.forName(PLUGIN_FQN.replace('#', '.'));
            isLibraryPresent = true;
        } catch (ClassNotFoundException ignored) {
        }

        if (!isLibraryAsPluginEnabled && isLibraryPresent) plugin.getLogger().warning(RELOCATION_MESSAGE);
    }
    // endregion

    /**
     * <b><i> This is an internal inventory-framework API that should not be used from outside of
     * this library. No compatibility guarantees are provided. </i></b>
     */
    @ApiStatus.Internal
    public final Viewer getViewer(@NotNull Player player) {
        return viewerById.get(player.getUniqueId().toString());
    }

    /**
     * Creates a new ViewFrame.
     *
     * @param owner The plugin that owns this view frame.
     * @return A new ViewFrame instance.
     */
    public static @NotNull ViewFrame create(@NotNull Plugin owner) {
        return new ViewFrame(owner);
    }

    /**
     * Installs a feature.
     *
     * @param feature   The feature to be installed.
     * @param configure The feature configuration.
     * @param <C>       The feature configuration type.
     * @param <R>       The feature value instance type.
     * @return An instance of the installed feature.
     */
    public final <C, R> ViewFrame install(
            @NotNull Feature<C, R, ViewFrame> feature, @NotNull UnaryOperator<C> configure) {
        featureInstaller.install(feature, configure);
        IFDebug.debug("Feature %s installed", feature.name());
        return this;
    }

    /**
     * Installs a feature with no specific configuration.
     *
     * @param feature The feature to be installed.
     * @return This view frame.
     */
    @NotNull
    public final ViewFrame install(@NotNull Feature<?, ?, ViewFrame> feature) {
        install(feature, UnaryOperator.identity());
        return this;
    }

    /**
     * Disables bStats metrics tracking.
     * <p>
     * InventoryFramework use bStats metrics to obtain some information from servers that use it as
     * a library, such as: number of players, version, software, etc.
     * <p>
     * **No sensitive information is tracked.**
     *
     * @return This view frame.
     */
    public final ViewFrame disableMetrics() {
        System.setProperty(BSTATS_SYSTEM_PROP, String.valueOf(Boolean.FALSE));
        return this;
    }
}
