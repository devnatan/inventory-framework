package me.devnatan.inventoryframework;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import me.devnatan.inventoryframework.context.CloseContext;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link View} whose lifecycle handlers are supplied as lambdas rather than by overriding methods
 * on a subclass. Instances are created and configured through {@link ViewBuilder}, never directly.
 * <p>
 * Unlike a regular {@link View} subclass, handlers aren't known upfront in a constructor, so they're
 * accumulated into these fields as {@link ViewBuilder}'s fluent methods are called, in the same
 * "add another handler" fashion regardless of call order.
 */
final class InlineView extends View {

    private final ViewConfigBuilder configBuilder;
    private Consumer<View> initializer;
    private Consumer<OpenContext> openHandler;
    private Consumer<RenderContext> firstRenderHandler;
    private Consumer<Context> updateHandler;
    private Consumer<CloseContext> closeHandler;
    private Consumer<SlotClickContext> clickHandler;
    private BiConsumer<Context, Context> resumeHandler;

    InlineView(@NotNull ViewConfigBuilder configBuilder) {
        this.configBuilder = configBuilder;
    }

    void addInitializer(@NotNull Consumer<View> handler) {
        initializer = initializer == null ? handler : initializer.andThen(handler);
    }

    void addOpenHandler(@NotNull Consumer<OpenContext> handler) {
        openHandler = openHandler == null ? handler : openHandler.andThen(handler);
    }

    void addFirstRenderHandler(@NotNull Consumer<RenderContext> handler) {
        firstRenderHandler = firstRenderHandler == null ? handler : firstRenderHandler.andThen(handler);
    }

    void addUpdateHandler(@NotNull Consumer<Context> handler) {
        updateHandler = updateHandler == null ? handler : updateHandler.andThen(handler);
    }

    void addCloseHandler(@NotNull Consumer<CloseContext> handler) {
        closeHandler = closeHandler == null ? handler : closeHandler.andThen(handler);
    }

    void addClickHandler(@NotNull Consumer<SlotClickContext> handler) {
        clickHandler = clickHandler == null ? handler : clickHandler.andThen(handler);
    }

    void addResumeHandler(@NotNull BiConsumer<Context, Context> handler) {
        resumeHandler = resumeHandler == null ? handler : resumeHandler.andThen(handler);
    }

    @Override
    public void onInit(@NotNull ViewConfigBuilder config) {
        config.inheritFrom(configBuilder);
        if (initializer != null) initializer.accept(this);
    }

    @Override
    public void onOpen(@NotNull OpenContext open) {
        if (openHandler != null) openHandler.accept(open);
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render) {
        if (firstRenderHandler != null) firstRenderHandler.accept(render);
    }

    @Override
    public void onUpdate(@NotNull Context update) {
        if (updateHandler != null) updateHandler.accept(update);
    }

    @Override
    public void onClose(@NotNull CloseContext close) {
        if (closeHandler != null) closeHandler.accept(close);
    }

    @Override
    public void onClick(@NotNull SlotClickContext click) {
        if (clickHandler != null) clickHandler.accept(click);
    }

    @Override
    public void onResume(@NotNull Context origin, @NotNull Context target) {
        if (resumeHandler != null) resumeHandler.accept(origin, target);
    }
}
