package me.devnatan.inventoryframework.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import me.devnatan.inventoryframework.VirtualView;
import me.devnatan.inventoryframework.component.ComponentFactory;
import me.devnatan.inventoryframework.context.IFRenderContext;
import me.devnatan.inventoryframework.exception.SlotFillExceededException;

/**
 * Resolves items registered through {@code row(int)}/{@code column(int)} to the next available
 * slot within the target row/column, in order, skipping slots that are already occupied.
 */
public final class RowColumnSlotInterceptor implements PipelineInterceptor<VirtualView> {

    @Override
    public void intercept(PipelineContext<VirtualView> pipeline, VirtualView subject) {
        if (!(subject instanceof IFRenderContext)) return;

        final IFRenderContext context = (IFRenderContext) subject;
        resolveRows(context);
        resolveColumns(context);
    }

    private void resolveRows(IFRenderContext context) {
        final int columnsCount = context.getContainer().getColumnsCount();

        for (final Map.Entry<Integer, List<BiFunction<Integer, Integer, ComponentFactory>>> entry :
                context.getRowSlotFactories().entrySet()) {
            final int row = entry.getKey();
            final int start = Math.max(row - 1, 0) * columnsCount;

            final List<Integer> candidateSlots = new ArrayList<>(columnsCount);
            for (int i = 0; i < columnsCount; i++) candidateSlots.add(start + i);

            resolveBounded(context, entry.getValue(), candidateSlots, "row", row);
        }
    }

    private void resolveColumns(IFRenderContext context) {
        final int columnsCount = context.getContainer().getColumnsCount();
        final int rowsCount = context.getContainer().getRowsCount();

        for (final Map.Entry<Integer, List<BiFunction<Integer, Integer, ComponentFactory>>> entry :
                context.getColumnSlotFactories().entrySet()) {
            final int column = entry.getKey();
            final int base = Math.max(column - 1, 0);

            final List<Integer> candidateSlots = new ArrayList<>(rowsCount);
            for (int i = 0; i < rowsCount; i++) candidateSlots.add(base + i * columnsCount);

            resolveBounded(context, entry.getValue(), candidateSlots, "column", column);
        }
    }

    private void resolveBounded(
            IFRenderContext context,
            List<BiFunction<Integer, Integer, ComponentFactory>> factories,
            List<Integer> candidateSlots,
            String axis,
            int axisIndex) {
        int cursor = 0;
        for (int i = 0; i < factories.size(); i++) {
            while (cursor < candidateSlots.size()
                    && AvailableSlotInterceptor.isSlotNotAvailableForAutoFilling(context, candidateSlots.get(cursor)))
                cursor++;

            if (cursor >= candidateSlots.size())
                throw new SlotFillExceededException(String.format(
                        "Capacity to accommodate items in %s %d has been exceeded (%d slots available, "
                                + "tried to fill item at index %d).",
                        axis, axisIndex, candidateSlots.size(), i));

            final int slot = candidateSlots.get(cursor++);
            context.addComponent(factories.get(i).apply(i, slot).create());
        }
    }
}
