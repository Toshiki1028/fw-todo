package com.example.todoapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class CategoryServiceTest {
    private final CategoryMapper mapper = mock(CategoryMapper.class);
    private final CategoryService service = new CategoryService(mapper);

    @Test
    void renameUpdatesExistingTodoNames() {
        Category category = category(7L, "旧ジャンル");
        when(mapper.findByIdForUpdate(7L)).thenReturn(category);

        Category renamed = service.rename(7L, " 新ジャンル ");

        assertEquals("新ジャンル", renamed.getName());
        verify(mapper).rename(7L, "新ジャンル");
        verify(mapper).renameTodos("旧ジャンル", "新ジャンル");
    }

    @Test
    void usedCategoryCannotBeDeleted() {
        when(mapper.findByIdForUpdate(7L)).thenReturn(category(7L, "使用中"));
        when(mapper.countTodos("使用中")).thenReturn(1);

        assertThrows(IllegalStateException.class, () -> service.delete(7L));
        verify(mapper, never()).deleteIfUnused(7L);
    }

    @Test
    void unusedCategoryCanBeDeleted() {
        when(mapper.findByIdForUpdate(7L)).thenReturn(category(7L, "未使用"));
        when(mapper.deleteIfUnused(7L)).thenReturn(1);

        service.delete(7L);

        verify(mapper).deleteIfUnused(7L);
    }

    @Test
    void blankAndDuplicateNamesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.create("  "));
        when(mapper.findByName("既存")).thenReturn(category(7L, "既存"));
        assertThrows(IllegalStateException.class, () -> service.create("既存"));
    }

    private Category category(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }
}
