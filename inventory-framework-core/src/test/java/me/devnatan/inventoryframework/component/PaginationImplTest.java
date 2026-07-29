package me.devnatan.inventoryframework.component;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import me.devnatan.inventoryframework.context.IFContext;
import me.devnatan.inventoryframework.context.IFSlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class PaginationImplTest {

    @SuppressWarnings("unchecked")
    private static PaginationImpl newComputedPagination() {
        final Supplier<List<Integer>> sourceProvider = () -> List.of(1, 2, 3);
        return new PaginationImpl(
                mock(State.class),
                mock(IFContext.class),
                'X',
                sourceProvider,
                mock(PaginationElementFactory.class),
                null,
                /* isAsync = */ false,
                /* isComputed = */ true,
                Pagination.Orientation.HORIZONTAL);
    }

    private static void setPagesCount(PaginationImpl pagination, int pagesCount) throws ReflectiveOperationException {
        final Field field = PaginationImpl.class.getDeclaredField("pagesCount");
        field.setAccessible(true);
        field.set(pagination, pagesCount);
    }

    private static void setCurrentPageIndex(PaginationImpl pagination, int pageIndex)
            throws ReflectiveOperationException {
        final Field field = PaginationImpl.class.getDeclaredField("currPageIndex");
        field.setAccessible(true);
        field.set(pagination, pageIndex);
    }

    @Test
    void computedPaginationCannotAdvanceOrBackWithoutMorePages() throws ReflectiveOperationException {
        final PaginationImpl pagination = newComputedPagination();
        setPagesCount(pagination, 1);
        setCurrentPageIndex(pagination, 0);

        // A single-page computed pagination must not report being able to advance or go back,
        // regression test for https://github.com/DevNatan/inventory-framework/issues/682
        assertFalse(pagination.canAdvance());
        assertFalse(pagination.canBack());
    }

    @Test
    void computedPaginationCanAdvanceAndBackWithinBounds() throws ReflectiveOperationException {
        final PaginationImpl pagination = newComputedPagination();
        setPagesCount(pagination, 3);

        setCurrentPageIndex(pagination, 0);
        assertTrue(pagination.canAdvance());
        assertFalse(pagination.canBack());

        setCurrentPageIndex(pagination, 1);
        assertTrue(pagination.canAdvance());
        assertTrue(pagination.canBack());

        setCurrentPageIndex(pagination, 2);
        assertFalse(pagination.canAdvance());
        assertTrue(pagination.canBack());
    }

    @Test
    void hasPageRejectsNegativeIndexEvenWhenComputed() throws ReflectiveOperationException {
        final PaginationImpl pagination = newComputedPagination();
        setPagesCount(pagination, 5);

        assertFalse(pagination.hasPage(-1));
    }

    @Test
    @Disabled("This test is not working as expected, because it is passed to parent")
    void callAnyChildInteractionHandlerOnClickInteraction() {
        PaginationImpl pagination = mock(PaginationImpl.class);
        doCallRealMethod().when(pagination).clicked(any(), any());

        Component child = mock(Component.class);
        InteractionHandler interactionHandler = mock(InteractionHandler.class);
        when(child.getInteractionHandler()).thenReturn(interactionHandler);
        when(child.isContainedWithin(0)).thenReturn(true);
        when(pagination.getInternalComponents()).thenReturn(Collections.singletonList(child));

        IFSlotClickContext clickContext = mock(IFSlotClickContext.class);
        when(clickContext.getClickedSlot()).thenReturn(0);
        pagination.clicked(pagination, clickContext);
        verify(interactionHandler, atLeastOnce()).clicked(pagination, clickContext);
    }

    @Test
    @Disabled("This test is not working as expected, because it is passed to parent")
    void callCorrectChildInteractionHandlerOnClickInteraction() {
        PaginationImpl pagination = mock(PaginationImpl.class);
        doCallRealMethod().when(pagination).clicked(any(), any());

        InteractionHandler child0InteractionHandler = mock(InteractionHandler.class);
        Component childAt0 = mock(Component.class);
        when(childAt0.isContainedWithin(0)).thenReturn(true);
        when(childAt0.isContainedWithin(1)).thenReturn(false);
        when(childAt0.getInteractionHandler()).thenReturn(child0InteractionHandler);

        InteractionHandler child1InteractionHandler = mock(InteractionHandler.class);
        Component childAt1 = mock(Component.class);
        when(childAt1.isContainedWithin(0)).thenReturn(false);
        when(childAt1.isContainedWithin(1)).thenReturn(true);
        when(childAt1.getInteractionHandler()).thenReturn(child1InteractionHandler);

        IFSlotClickContext clickContext = mock(IFSlotClickContext.class);
        when(clickContext.getClickedSlot()).thenReturn(0);
        pagination.clicked(pagination, clickContext);
        verify(child0InteractionHandler, times(1)).clicked(pagination, clickContext);
        verify(child1InteractionHandler, never()).clicked(pagination, clickContext);
    }
}
