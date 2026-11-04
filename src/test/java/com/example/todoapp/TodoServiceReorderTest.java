package com.example.todoapp;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class TodoServiceReorderTest {
    private final TodoMapper mapper = mock(TodoMapper.class);
    private final TodoService service = new TodoService(mapper, mock(CategoryService.class));

    @Test
    void reordersOnlyVisibleSlotsAndPreservesHiddenAndOtherPages() {
        Todo pinned = todo(1, true);
        Todo firstPage = todo(2, false);
        Todo hidden = todo(3, false);
        Todo secondPage = todo(4, false);
        Todo last = todo(5, false);
        when(mapper.lockManualOrder()).thenReturn(List.of(pinned, firstPage, hidden, secondPage, last));
        when(mapper.searchPage("work", "", "manual", false, false, 2, 2))
                .thenReturn(List.of(secondPage, last));

        assertTrue(service.reorderPage(List.of(5L, 4L), "work", "", false, 2, 2));

        verify(mapper).setSortPosition(2L, 4);
        verify(mapper).setSortPosition(3L, 3);
        verify(mapper).setSortPosition(5L, 2);
        verify(mapper).setSortPosition(4L, 1);
        verify(mapper, never()).setSortPosition(eq(1L), anyLong());
    }

    @Test
    void rejectsMovingAcrossPinnedBoundary() {
        Todo pinned = todo(1, true);
        Todo ordinary = todo(2, false);
        when(mapper.lockManualOrder()).thenReturn(List.of(pinned, ordinary));
        when(mapper.searchPage("", "", "manual", true, false, 10, 0))
                .thenReturn(List.of(pinned, ordinary));

        assertFalse(service.reorderPage(List.of(2L, 1L), "", "", true, 1, 10));
        verify(mapper, never()).setSortPosition(anyLong(), anyLong());
    }

    private Todo todo(long id, boolean pinned) {
        Todo todo = new Todo();
        todo.setId(id);
        todo.setPinned(pinned);
        return todo;
    }
}
