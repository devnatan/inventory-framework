package me.devnatan.inventoryframework.component;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import me.devnatan.inventoryframework.Ref;
import me.devnatan.inventoryframework.ViewContainer;
import me.devnatan.inventoryframework.VirtualView;
import me.devnatan.inventoryframework.bukkit.context.*;
import me.devnatan.inventoryframework.context.*;
import me.devnatan.inventoryframework.state.State;
import me.devnatan.inventoryframework.utils.SlotConverter;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BukkitItemComponentBuilder extends DefaultComponentBuilder<BukkitItemComponentBuilder, Context>
        implements ItemComponentBuilder<BukkitItemComponentBuilder, Context>, ComponentFactory {

    private final VirtualView root;
    private int slot;
    private ItemStack item;
    private Consumer<? super IFSlotRenderContext> renderHandler;
    private Consumer<? super IFSlotClickContext> clickHandler;
    private Consumer<? super IFSlotContext> updateHandler;

    public BukkitItemComponentBuilder(VirtualView root) {
        this(
                null,
                root,
                -1,
                null,
                null,
                null,
                null,
                null,
                new HashMap<>(),
                false,
                false,
                false,
                new LinkedHashSet<>(),
                false,
                null);
    }

    private BukkitItemComponentBuilder(
            Function<? extends IFContext, String> keyFactory,
            VirtualView root,
            int slot,
            ItemStack item,
            Consumer<? super IFSlotRenderContext> renderHandler,
            Consumer<? super IFSlotClickContext> clickHandler,
            Consumer<? super IFSlotContext> updateHandler,
            Ref<Component> reference,
            Map<String, Object> data,
            boolean cancelOnClick,
            boolean closeOnClick,
            boolean updateOnClick,
            Set<State<?>> watchingStates,
            boolean isManagedExternally,
            Predicate<Context> displayCondition) {
        super(
                keyFactory,
                reference,
                data,
                cancelOnClick,
                closeOnClick,
                updateOnClick,
                watchingStates,
                isManagedExternally,
                displayCondition);
        this.root = root;
        this.slot = slot;
        this.item = item;
        this.renderHandler = renderHandler;
        this.clickHandler = clickHandler;
        this.updateHandler = updateHandler;
    }

    @Override
    public String toString() {
        return "BukkitItemComponentBuilder{"
                + "slot=" + slot
                + ", item=" + item
                + ", renderHandler=" + renderHandler
                + ", clickHandler=" + clickHandler
                + ", updateHandler=" + updateHandler
                + "} " + super.toString();
    }

    @Override
    public boolean isContainedWithin(int position) {
        return position == slot;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BukkitItemComponentBuilder withSlot(int slot) {
        this.slot = slot;
        return this;
    }

    @Override
    public BukkitItemComponentBuilder withSlot(int row, int column) {
        final ViewContainer container = ((IFRenderContext) root).getContainer();
        return withSlot(SlotConverter.convertSlot(row, column, container.getRowsCount(), container.getColumnsCount()));
    }

    /**
     * Defines the item that will be used as fallback for rendering in the slot where this item is
     * positioned. The fallback item is always static.
     *
     * @param item The new fallback item stack.
     * @return This item builder.
     */
    public BukkitItemComponentBuilder withItem(@Nullable ItemStack item) {
        this.item = item;
        return this;
    }

    /**
     * Called when the item is rendered.
     * <p>
     * This handler is called every time the item or the view that owns it is updated.
     *
     * @param renderHandler The render handler.
     * @return This item builder.
     */
    @SuppressWarnings("unchecked")
    public BukkitItemComponentBuilder onRender(@Nullable Consumer<? super SlotRenderContext> renderHandler) {
        this.renderHandler = (Consumer<? super IFSlotRenderContext>) renderHandler;
        return this;
    }

    /**
     * Dynamic rendering of a specific item.
     * <p>
     * This handler is called every time the item or the view that owns it is updated.
     *
     * @param renderFactory The render handler.
     * @return This item builder.
     */
    public BukkitItemComponentBuilder renderWith(@NotNull Supplier<@Nullable ItemStack> renderFactory) {
        return onRender(render -> render.setItem(renderFactory.get()));
    }

    /**
     * Called when a player clicks on the item.
     * <p>
     * This handler works on any container that the actor has access to and only works if the
     * interaction has not been cancelled.
     *
     * @param clickHandler The click handler.
     * @return This item builder.
     */
    @SuppressWarnings("unchecked")
    public BukkitItemComponentBuilder onClick(@Nullable Consumer<? super SlotClickContext> clickHandler) {
        this.clickHandler = (Consumer<? super IFSlotClickContext>) clickHandler;
        return this;
    }

    /**
     * Called when a player clicks on the item.
     * <p>
     * This handler works on any container that the actor has access to and only works if the
     * interaction has not been cancelled.
     *
     * @param clickHandler The click handler.
     * @return This item builder.
     */
    public BukkitItemComponentBuilder onClick(@Nullable Runnable clickHandler) {
        return onClick(clickHandler == null ? null : $ -> clickHandler.run());
    }

    /**
     * Called when the item is updated.
     *
     * @param updateHandler The update handler.
     * @return This item builder.
     */
    @SuppressWarnings("unchecked")
    public BukkitItemComponentBuilder onUpdate(@Nullable Consumer<? super SlotContext> updateHandler) {
        this.updateHandler = (Consumer<? super IFSlotContext>) updateHandler;
        return this;
    }

    @Override
    public @NotNull Component create() {
        final Function<? extends IFContext, String> componentKeyProvider =
                keyFactory == null ? RANDOM_KEY_FACTORY : keyFactory;
        return new ItemComponent(
                componentKeyProvider,
                root,
                slot,
                item,
                cancelOnClick,
                closeOnClick,
                displayCondition,
                renderHandler,
                updateHandler,
                clickHandler,
                watchingStates,
                isManagedExternally,
                updateOnClick,
                false,
                reference);
    }

    @Override
    public BukkitItemComponentBuilder copy() {
        return new BukkitItemComponentBuilder(
                keyFactory,
                root,
                slot,
                item,
                renderHandler,
                clickHandler,
                updateHandler,
                reference,
                data,
                cancelOnClick,
                closeOnClick,
                updateOnClick,
                watchingStates,
                isManagedExternally,
                displayCondition);
    }
}
