package me.devnatan.inventoryframework;

import static java.util.Objects.requireNonNull;
import static me.devnatan.inventoryframework.AnvilInput.defaultConfig;
import static me.devnatan.inventoryframework.IFViewFrame.FRAME_REGISTERED;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;
import me.devnatan.inventoryframework.bukkit.ViewFrame;
import me.devnatan.inventoryframework.bukkit.context.CloseContext;
import me.devnatan.inventoryframework.bukkit.context.OpenContext;
import me.devnatan.inventoryframework.bukkit.context.SlotClickContext;
import me.devnatan.inventoryframework.context.IFCloseContext;
import me.devnatan.inventoryframework.context.IFContext;
import me.devnatan.inventoryframework.context.IFOpenContext;
import me.devnatan.inventoryframework.context.IFRenderContext;
import me.devnatan.inventoryframework.context.IFSlotClickContext;
import me.devnatan.inventoryframework.feature.Feature;
import me.devnatan.inventoryframework.pipeline.PipelineInterceptor;
import me.devnatan.inventoryframework.pipeline.StandardPipelinePhases;
import me.devnatan.inventoryframework.state.State;
import me.devnatan.inventoryframework.state.StateValue;
import me.devnatan.inventoryframework.state.StateValueHost;
import me.devnatan.inventoryframework.state.StateWatcher;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"rawtypes", "unchecked", "unused"})
public final class AnvilInputFeature implements Feature<AnvilInputConfig, Void, ViewFrame> {

    private static final int INGREDIENT_SLOT = 0;

    /**
     * Instance of the Anvil Input feature.
     *
     * @see <a href="https://github.com/DevNatan/inventory-framework/wiki/anvil-input">Anvil Input on Wiki</a>
     */
    public static final Feature<AnvilInputConfig, Void, ViewFrame> AnvilInput = new AnvilInputFeature();

    private AnvilInputConfig config;
    private PipelineInterceptor frameInterceptor;

    private AnvilInputFeature() {}

    @Override
    public @NotNull String name() {
        return "Anvil Input";
    }

    @Override
    public @NotNull Void install(ViewFrame framework, UnaryOperator<AnvilInputConfig> configure) {
        config = configure.apply(defaultConfig());
        framework.getPipeline().intercept(FRAME_REGISTERED, (frameInterceptor = createFrameworkInterceptor()));
        return null;
    }

    @Override
    public void uninstall(ViewFrame framework) {
        framework.getPipeline().removeInterceptor(FRAME_REGISTERED, frameInterceptor);
    }

    private PipelineInterceptor createFrameworkInterceptor() {
        return (PipelineInterceptor<IFViewFrame>) (pipeline, subject) -> {
            final Map<UUID, PlatformView> views = subject.getRegisteredViews();

            for (final PlatformView view : views.values()) {
                handleOpen(view);
                handleClose(view);
                handleClick(view);
            }
        };
    }

    private AnvilInput getAnvilInput(IFContext context) {
        if (context.getConfig().getType() != ViewType.ANVIL) return null;

        final Optional<ViewConfig.Modifier> optional = context.getConfig().getModifiers().stream()
                .filter(modifier -> modifier instanceof AnvilInput)
                .findFirst();

        //noinspection OptionalIsPresent
        if (!optional.isPresent()) return null;

        return (AnvilInput) optional.get();
    }

    private void updatePhysicalResult(String newText, ViewContainer container) {
        final Inventory inventory = ((BukkitViewContainer) container).getInventory();
        final ItemStack ingredientItem = requireNonNull(inventory.getItem(INGREDIENT_SLOT));
        final ItemMeta ingredientMeta = requireNonNull(ingredientItem.getItemMeta());
        ingredientMeta.setDisplayName(newText);
        ingredientItem.setItemMeta(ingredientMeta);
    }

    private void handleClick(PlatformView view) {
        view.getPipeline().intercept(StandardPipelinePhases.CLICK, (pipeline, subject) -> {
            if (!(subject instanceof IFSlotClickContext)) return;

            final SlotClickContext context = (SlotClickContext) subject;
            final AnvilInput anvilInput = getAnvilInput(context);
            if (anvilInput == null) return;

            final int resultSlot = context.getContainer().getType().getResultSlots()[0];
            if (context.getClickedSlot() != resultSlot) return;

            final ItemStack resultItem = context.getItem();
            if (resultItem == null || resultItem.getType() == Material.AIR) return;

            final ItemMeta resultMeta = requireNonNull(resultItem.getItemMeta());
            final String text = resultMeta.getDisplayName();
            final Inventory clickedInventory =
                    requireNonNull(context.getClickOrigin().getClickedInventory(), "Clicked inventory cannot be null");
            final ItemStack ingredientItem = requireNonNull(clickedInventory.getItem(INGREDIENT_SLOT));
            final ItemMeta ingredientMeta = requireNonNull(ingredientItem.getItemMeta());
            ingredientMeta.setDisplayName(text);
            context.updateState(anvilInput, text);
            ingredientItem.setItemMeta(ingredientMeta);

            if (config.closeOnSelect) {
                context.closeForPlayer();
            }
        });
    }

    private void handleOpen(PlatformView view) {
        view.getPipeline().intercept(StandardPipelinePhases.OPEN, (pipeline, subject) -> {
            if (!(subject instanceof IFOpenContext)) return;

            final OpenContext context = (OpenContext) subject;
            final AnvilInput anvilInput = getAnvilInput(context);
            if (anvilInput == null) return;

            // Forces internal state initialization
            context.getInternalStateValue(anvilInput);
            context.watchState(anvilInput.internalId(), new StateWatcher() {
                @Override
                public void stateRegistered(@NotNull State<?> state, Object caller) {}

                @Override
                public void stateUnregistered(@NotNull State<?> state, Object caller) {}

                @Override
                public void stateValueGet(
                        @NotNull State<?> state,
                        @NotNull StateValueHost host,
                        @NotNull StateValue internalValue,
                        Object rawValue) {}

                @Override
                public void stateValueSet(
                        @NotNull StateValueHost host,
                        @NotNull StateValue value,
                        Object rawOldValue,
                        Object rawNewValue) {
                    updatePhysicalResult((String) rawNewValue, ((IFRenderContext) host).getContainer());
                }
            });

            final String globalInitialInput = config.initialInput;
            final String scopedInitialInput = anvilInput.get(context);

            final Inventory inventory = AnvilInputNMS.open(
                    context.getPlayer(),
                    context.getConfig().getTitle(),
                    scopedInitialInput.isEmpty() ? globalInitialInput : scopedInitialInput);
            final ViewContainer container =
                    new BukkitViewContainer(inventory, context.isShared(), ViewType.ANVIL, true);

            context.setContainer(container);
        });
    }

    private void handleClose(PlatformView view) {
        view.getPipeline().intercept(StandardPipelinePhases.CLOSE, (pipeline, subject) -> {
            if (!(subject instanceof IFCloseContext)) return;

            final CloseContext context = (CloseContext) subject;
            final AnvilInput anvilInput = getAnvilInput(context);
            if (anvilInput == null) return;

            final BukkitViewContainer container = (BukkitViewContainer) context.getContainer();
            final int slot = container.getType().getResultSlots()[0];
            final ItemStack item = container.getInventory().getItem(slot);

            if (item == null || item.getType() == Material.AIR) return;

            final String input = requireNonNull(item.getItemMeta()).getDisplayName();
            context.updateState(anvilInput, input);
        });
    }
}
