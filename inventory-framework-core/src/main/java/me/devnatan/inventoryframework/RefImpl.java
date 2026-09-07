package me.devnatan.inventoryframework;

import me.devnatan.inventoryframework.context.IFRenderContext;
import org.jetbrains.annotations.NotNull;

final class RefImpl<E> implements Ref<E> {

    private static final Object UNASSIGNED_VALUE = new Object();

    private Object assignment = UNASSIGNED_VALUE;

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull E of(@NotNull IFRenderContext context) {
        if (assignment == UNASSIGNED_VALUE) throw new UnassignedReferenceException();

        return (E) assignment;
    }

    @Override
    public void assign(@NotNull IFRenderContext context, Object value) {
        if (assignment != UNASSIGNED_VALUE) throw new IllegalStateException("Reference cannot be reassigned");

        this.assignment = value;
    }
}
