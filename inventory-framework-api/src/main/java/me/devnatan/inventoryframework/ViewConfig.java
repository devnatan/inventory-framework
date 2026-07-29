package me.devnatan.inventoryframework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import me.devnatan.inventoryframework.context.IFContext;
import me.devnatan.inventoryframework.state.timer.TimerState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

@VisibleForTesting
@ApiStatus.NonExtendable
public class ViewConfig {

    public static final ViewConfig.Option<Boolean> CANCEL_ON_CLICK = createOption("cancel-on-click", true);
    public static final ViewConfig.Option<Boolean> CANCEL_ON_PICKUP = createOption("cancel-on-pickup", true);
    public static final ViewConfig.Option<Boolean> CANCEL_ON_DROP = createOption("cancel-on-drop", true);
    public static final ViewConfig.Option<Boolean> CANCEL_ON_DRAG = createOption("cancel-on-drag", true);

    private final Object title;
    private final int size;
    private final ViewType type;
    private final Map<Option<?>, Object> options;
    private final String[] layout;
    private final Set<Modifier> modifiers;
    private final long updateIntervalInTicks, interactionDelayInMillis;
    private final boolean transitiveInitialData;
    private final TimerState updateIntervalState;

    public ViewConfig(
            Object title,
            int size,
            ViewType type,
            Map<Option<?>, Object> options,
            String[] layout,
            Set<Modifier> modifiers,
            long updateIntervalInTicks,
            long interactionDelayInMillis,
            boolean transitiveInitialData,
            TimerState updateIntervalState) {
        this.title = title;
        this.size = size;
        this.type = type;
        this.options = options;
        this.layout = layout;
        this.modifiers = modifiers;
        this.updateIntervalInTicks = updateIntervalInTicks;
        this.interactionDelayInMillis = interactionDelayInMillis;
        this.transitiveInitialData = transitiveInitialData;
        this.updateIntervalState = updateIntervalState;
    }

    public Object getTitle() {
        return title;
    }

    public int getSize() {
        return size;
    }

    public ViewType getType() {
        return type;
    }

    public Map<Option<?>, Object> getOptions() {
        return options;
    }

    public String[] getLayout() {
        return layout;
    }

    public Set<Modifier> getModifiers() {
        return modifiers;
    }

    public long getUpdateIntervalInTicks() {
        return updateIntervalInTicks;
    }

    public long getInteractionDelayInMillis() {
        return interactionDelayInMillis;
    }

    public boolean isTransitiveInitialData() {
        return transitiveInitialData;
    }

    @Nullable
    public TimerState getUpdateIntervalState() {
        return updateIntervalState;
    }

    @VisibleForTesting
    @SuppressWarnings("unchecked")
    public <T> T getOptionValue(@NotNull Option<T> option) {
        for (final Map.Entry<Option<?>, Object> entry : getOptions().entrySet()) {
            final Option<?> other = entry.getKey();
            if (other.name().equals(option.name())) return (T) entry.getValue();
        }

        throw new IllegalArgumentException("Unknown option: " + option);
    }

    @VisibleForTesting
    public <T> boolean isOptionSet(@NotNull Option<T> option) {
        for (final Map.Entry<Option<?>, Object> entry : getOptions().entrySet()) {
            final Option<?> other = entry.getKey();
            final Object definedValue = entry.getValue();
            if (other.name().equals(option.name()) && Objects.equals(definedValue, other.defaultValue())) return true;
        }
        return false;
    }

    // TODO docs
    @SuppressWarnings("unused")
    @VisibleForTesting
    public <T> boolean isOptionSet(@NotNull Option<T> option, T value) {
        for (final Map.Entry<Option<?>, Object> entry : getOptions().entrySet()) {
            final Option<?> other = entry.getKey();
            final Object definedValue = entry.getValue();
            if (other.name().equals(option.name()) && Objects.equals(definedValue, value)) return true;
        }
        return false;
    }

    public static <T> Option<T> createOption(@NotNull String name, @NotNull T defaultValue) {
        return new OptionImpl<>(name, defaultValue);
    }

    /**
     * Merges other config into this configuration.
     *
     * @param other The configuration to be merged.
     * @return A ViewConfig with both {@code this} and the other configuration.
     */
    @ApiStatus.Internal
    @VisibleForTesting
    public ViewConfig merge(ViewConfig other) {
        if (other == null) return this;

        // TODO merge "options" and "modifiers" from both, distinctly
        return new ViewConfig(
                merge(other, ViewConfig::getTitle, Objects::nonNull),
                merge(other, ViewConfig::getSize, value -> value != 0),
                merge(other, ViewConfig::getType),
                merge(other, ViewConfig::getOptions, value -> value != null && !value.isEmpty()),
                merge(other, ViewConfig::getLayout),
                merge(other, ViewConfig::getModifiers, value -> value != null && !value.isEmpty()),
                merge(other, ViewConfig::getUpdateIntervalInTicks, value -> value != 0),
                merge(other, ViewConfig::getInteractionDelayInMillis, value -> value != 0),
                merge(other, ViewConfig::isTransitiveInitialData),
                merge(other, ViewConfig::getUpdateIntervalState, Objects::nonNull));
    }

    private <T> T merge(ViewConfig other, Function<ViewConfig, T> retriever) {
        return merge(other, retriever, Objects::nonNull);
    }

    private <T> T merge(ViewConfig other, Function<ViewConfig, T> retriever, Function<T, Boolean> mergeCondition) {
        T value = retriever.apply(this);
        if (!mergeCondition.apply(value)) return retriever.apply(other);
        return value;
    }

    // TODO docs
    @SuppressWarnings("unused")
    @FunctionalInterface
    public interface Modifier {

        /**
         * Applies this modifier to a given configuration.
         *
         * @param config The target configuration.
         */
        void apply(@NotNull ViewConfigBuilder config, @NotNull IFContext context);
    }

    public interface Option<T> {

        @NotNull
        String name();

        @Nullable
        T defaultValue();
    }

    private static final class OptionImpl<T> implements Option<T> {
        private static final List<String> registeredNames = new ArrayList<>();

        private final String name;
        private final T defaultValue;

        OptionImpl(@NotNull String name, T defaultValue) {
            if (registeredNames.contains(name))
                throw new IllegalStateException(String.format("Option %s already registered", name));

            this.name = name;
            this.defaultValue = defaultValue;
        }

        @Override
        public @NotNull String name() {
            return name;
        }

        @Override
        public @Nullable T defaultValue() {
            return defaultValue;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OptionImpl<?> option = (OptionImpl<?>) o;
            return Objects.equals(name, option.name) && Objects.equals(defaultValue, option.defaultValue);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, defaultValue);
        }

        @Override
        public String toString() {
            return "OptionImpl{" + "name='" + name + '\'' + ", defaultValue=" + defaultValue + '}';
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ViewConfig that = (ViewConfig) o;
        return getSize() == that.getSize()
                && getUpdateIntervalInTicks() == that.getUpdateIntervalInTicks()
                && getInteractionDelayInMillis() == that.getInteractionDelayInMillis()
                && Objects.equals(getTitle(), that.getTitle())
                && Objects.equals(getType(), that.getType())
                && Objects.equals(getOptions(), that.getOptions())
                && Arrays.equals(getLayout(), that.getLayout())
                && Objects.equals(getModifiers(), that.getModifiers())
                && isTransitiveInitialData() == that.isTransitiveInitialData()
                && Objects.equals(getUpdateIntervalState(), that.getUpdateIntervalState());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(
                getTitle(),
                getSize(),
                getType(),
                getOptions(),
                getModifiers(),
                getUpdateIntervalInTicks(),
                getInteractionDelayInMillis(),
                isTransitiveInitialData(),
                getUpdateIntervalState());
        result = 31 * result + Arrays.hashCode(getLayout());
        return result;
    }

    @Override
    public String toString() {
        return "ViewConfig{" + "title="
                + title + ", size="
                + size + ", type="
                + type + ", options="
                + options + ", layout="
                + Arrays.toString(layout) + ", modifiers="
                + modifiers + ", updateIntervalInTicks="
                + updateIntervalInTicks + ", interactionDelayInMillis="
                + interactionDelayInMillis + ", transitiveInitialData="
                + transitiveInitialData + ", updateIntervalState="
                + updateIntervalState + "}";
    }
}
