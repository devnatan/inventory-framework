package me.devnatan.inventoryframework;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import me.devnatan.inventoryframework.exception.InvalidLayoutException;
import me.devnatan.inventoryframework.state.timer.TimerState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public final class ViewConfigBuilder {

    private static boolean titleAsComponentSupported;

    static {
        try {
            Class.forName("net.kyori.adventure.text.TextComponent");
            titleAsComponentSupported = true;
        } catch (ClassNotFoundException ignored) {
            titleAsComponentSupported = false;
        }
    }

    private Object title;
    private int size = 0;
    private ViewType type;
    private final Set<ViewConfig.Option<?>> options = new HashSet<>();
    private String[] layout = null;
    private final Set<ViewConfig.Modifier> modifiers = new HashSet<>();
    private long updateIntervalInTicks, interactionDelayInMillis;
    private TimerState updateIntervalState;
    private boolean transitiveInitialData;

    /**
     * Inherits all configuration from another {@link ViewConfigBuilder} value.
     * <p>
     * Note that the values will be merged and not replaced, however, the values of the setting to
     * be inherited take precedence over those of that setting.
     *
     * @param other The configuration that will be inherited.
     * @return This config.
     */
    public ViewConfigBuilder inheritFrom(@NotNull ViewConfigBuilder other) {
        throw new UnsupportedOperationException("Inheritance is not yet supported");
    }

    /**
     * Defines the type of the container.
     * <p>
     * If applied in view scope, it will be the default value for all contexts originated from it.
     *
     * @param type The container type.
     * @return This config.
     */
    public ViewConfigBuilder type(ViewType type) {
        this.type = type;
        return this;
    }

    /**
     * Defines the title of the container.
     * <p>
     * <a href="https://github.com/KyoriPowered/adventure">Kyori's Adventure Text Component</a> is supported if your platform is PaperSpigot
     * in a non-legacy version. Non-{@link String} titles will be converted to a plain text.
     *
     * @param title The container title.
     * @return This configuration builder.
     */
    public ViewConfigBuilder title(Object title) {
        this.title = title;
        return this;
    }

    /**
     * Defines the size of the container.
     * <p>
     * If applied in view scope, it will be the default value for all contexts originated from it.
     *
     * @param size The container size.
     * @return This configuration builder.
     */
    public ViewConfigBuilder size(int size) {
        this.size = size;
        return this;
    }

    // TODO needs documentation
    public ViewConfigBuilder maxSize() {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Adds a modifier to this config.
     *
     * @param modifier The modifier that'll be added.
     * @return This configuration builder.
     */
    public ViewConfigBuilder with(@NotNull ViewConfig.Modifier modifier) {
        return use(modifier);
    }

    /**
     * Adds a modifier to this config.
     *
     * @param modifiers Modifiers to add.
     * @return This configuration builder.
     */
    public ViewConfigBuilder with(ViewConfig.Modifier... modifiers) {
        return use(modifiers);
    }

    /**
     * Uses a modifier in this config.
     *
     * @param modifier The modifiers to use.
     * @return This configuration builder.
     */
    public ViewConfigBuilder use(@NotNull ViewConfig.Modifier modifier) {
        this.modifiers.add(modifier);
        return this;
    }

    /**
     * Uses a modifier in this config.
     *
     * @param modifiers Modifiers to  use.
     * @return This configuration builder.
     */
    public ViewConfigBuilder use(ViewConfig.Modifier... modifiers) {
        this.modifiers.addAll(Arrays.asList(modifiers));
        return this;
    }

    /**
     * Defines the layout that will be used.
     *
     * @param layout The layout.
     * @return This configuration builder.
     * @throws InvalidLayoutException If the layout does not respect the container contracts of the
     *                                context in which it was applied (e.g. if the layout size
     *                                differs from the container size).
     */
    public ViewConfigBuilder layout(String... layout) {
        this.layout = layout;
        return this;
    }

    public ViewConfigBuilder options(ViewConfig.Option<?>... options) {
        this.options.addAll(Arrays.asList(options));
        return this;
    }

    private ViewConfigBuilder addOption(ViewConfig.Option<?> option) {
        options.add(option);
        return this;
    }

    public ViewConfigBuilder cancelOnClick() {
        return addOption(ViewConfig.CANCEL_ON_CLICK);
    }

    /**
     * Cancels any item pickup by the player while the view is open.
     *
     * @return This configuration builder.
     */
    public ViewConfigBuilder cancelOnPickup() {
        return addOption(ViewConfig.CANCEL_ON_PICKUP);
    }

    /**
     * Cancels any item drops by the player while the view is open.
     *
     * @return This configuration builder.
     */
    public ViewConfigBuilder cancelOnDrop() {
        return addOption(ViewConfig.CANCEL_ON_DROP);
    }

    /**
     * Cancels any item drag into the view.
     *
     * @return This configuration builder.
     */
    public ViewConfigBuilder cancelOnDrag() {
        return addOption(ViewConfig.CANCEL_ON_DRAG);
    }

    /**
     * Schedules the view to update every fixed interval.
     *
     * @param intervalInTicks The interval in ticks.
     * @return This configuration builder.
     * @see <a href="https://github.com/DevNatan/inventory-framework/wiki/scheduled-updates">Scheduled Updates on Wiki</a>
     */
    public ViewConfigBuilder scheduleUpdate(long intervalInTicks) {
        this.updateIntervalInTicks = intervalInTicks;
        return this;
    }

    /**
     * Schedules the view to update on every {@link TimerState timer} loop.
     * <p>
     * Unlike {@link #scheduleUpdate(long)}, the given timer can have its interval changed and be
     * paused and resumed at runtime, without opening a new view.
     *
     * <p><b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     *
     * @param timerState The timer state that will drive the scheduled updates.
     * @return This configuration builder.
     * @see <a href="https://github.com/DevNatan/inventory-framework/wiki/scheduled-updates">Scheduled Updates on Wiki</a>
     */
    @ApiStatus.Experimental
    public ViewConfigBuilder scheduleUpdate(@NotNull TimerState timerState) {
        this.updateIntervalState = timerState;
        return this;
    }

    /**
     * Waits a fixed delay before any player interaction.
     * <p>
     * Interactions called before delay completion are cancelled.
     *
     * <p><b><i> This API is experimental and is not subject to the general compatibility guarantees
     * such API may be changed or may be removed completely in any further release. </i></b>
     *
     * @param interactionDelay Duration of the interaction delay or <code>null</code> to reset.
     * @return This configuration builder.
     */
    @ApiStatus.Experimental
    public ViewConfigBuilder interactionDelay(Duration interactionDelay) {
        this.interactionDelayInMillis = interactionDelay == null ? 0 : interactionDelay.toMillis();
        return this;
    }

    /**
     * When navigating between views data from "A" to "B" is not carried so trying to access
     * some data from "A" in "B" will throw a {@link NullPointerException}.
     * <p>
     * This behavior can be changed by enabling this option, once enabled, on every navigation
     * between views data will be carried from the view where came from to the view where are going.
     *
     * @return This configuration builder.
     * @see <a href="https://github.com/DevNatan/inventory-framework/wiki/navigating-between-views">Navigating Between Views on Wiki</a>
     */
    public ViewConfigBuilder transitiveInitialData(boolean transitiveInitialData) {
        this.transitiveInitialData = transitiveInitialData;
        return this;
    }

    /**
     * Cancels click, pickup, drop and drag interactions with the view.
     *
     * @return This configuration builder.
     */
    public ViewConfigBuilder cancelInteractions() {
        return cancelOnClick().cancelOnPickup().cancelOnDrop().cancelOnDrag();
    }

    public ViewConfig build() {
        final Map<ViewConfig.Option<?>, Object> optionsMap = getOptions().stream()
                .map(option -> new AbstractMap.SimpleImmutableEntry<ViewConfig.Option<?>, Object>(
                        option, option.defaultValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return new ViewConfig(
                getTitle(),
                getSize(),
                getType(),
                optionsMap,
                getLayout(),
                getModifiers(),
                getUpdateIntervalInTicks(),
                getInteractionDelayInMillis(),
                transitiveInitialData,
                getUpdateIntervalState());
    }

    public static boolean isTitleAsComponentSupported() {
        return titleAsComponentSupported;
    }

    Object getTitle() {
        return title;
    }

    int getSize() {
        return size;
    }

    ViewType getType() {
        return type;
    }

    Set<ViewConfig.Option<?>> getOptions() {
        return options;
    }

    String[] getLayout() {
        return layout;
    }

    Set<ViewConfig.Modifier> getModifiers() {
        return modifiers;
    }

    long getUpdateIntervalInTicks() {
        return updateIntervalInTicks;
    }

    TimerState getUpdateIntervalState() {
        return updateIntervalState;
    }

    long getInteractionDelayInMillis() {
        return interactionDelayInMillis;
    }

    public boolean isTransitiveInitialData() {
        return transitiveInitialData;
    }
}
