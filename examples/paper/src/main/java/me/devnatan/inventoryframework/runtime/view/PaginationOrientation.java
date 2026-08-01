package me.devnatan.inventoryframework.runtime.view;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.bukkit.View;
import me.devnatan.inventoryframework.bukkit.context.RenderContext;
import me.devnatan.inventoryframework.component.Pagination;
import me.devnatan.inventoryframework.runtime.ExampleUtil;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public class PaginationOrientation extends View {

    private final State<Pagination> paginationState = lazyPaginationState(
            () -> IntStream.range(0, 50).boxed().collect(Collectors.toList()), (context, builder, index, value) -> {
                builder.withItem(ExampleUtil.displayItem(Material.ARROW, "Item " + value, value + 1));
                builder.onClick((ctx) -> {
                    ctx.getPlayer().sendMessage("You clicked on item " + index);
                });
            });

    @Override
    public void onInit(@NotNull ViewConfigBuilder config) {
        config.cancelOnClick();
        config.size(6);
        config.title("Pagination (HORIZONTAL)");
        config.layout("         ", "         ", "OOOOOOOOO", "OOOOOOOOO", "OOOOOOOOO", "OOOOOOOOO");
    }

    @Override
    public void onFirstRender(@NotNull RenderContext render) {
        render.firstSlot(ExampleUtil.displayItem(Material.DIAMOND, "Change orientation"))
                .onClick(click -> {
                    final Pagination pagination = paginationState.get(click);
                    pagination.setOrientation(pagination.getOrientation());

                    switch (pagination.getOrientation()) {
                        case VERTICAL:
                            pagination.setOrientation(Pagination.Orientation.HORIZONTAL);
                            break;
                        case HORIZONTAL:
                            pagination.setOrientation(Pagination.Orientation.ALTERNATING_COLUMNS);
                            break;
                        case ALTERNATING_COLUMNS:
                            pagination.setOrientation(Pagination.Orientation.ALTERNATING_ROWS);
                            break;
                        case ALTERNATING_ROWS:
                            pagination.setOrientation(Pagination.Orientation.TOP_BOTTOM_LEFT_RIGHT);
                            break;
                        case TOP_BOTTOM_LEFT_RIGHT:
                            pagination.setOrientation(Pagination.Orientation.VERTICAL);
                            break;
                    }

                    pagination.forceUpdate();
                    click.updateTitleForPlayer(
                            "Pagination (" + pagination.getOrientation().name() + ")");
                    click.getPlayer().sendMessage("Pagination orientation set to " + pagination.getOrientation());
                });
    }
}
