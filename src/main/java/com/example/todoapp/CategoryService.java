package com.example.todoapp;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {
    private final CategoryMapper mapper;

    public CategoryService(CategoryMapper mapper) {
        this.mapper = mapper;
    }

    public List<Category> findAll() {
        return mapper.findAll();
    }

    public boolean exists(String name) {
        return name != null && mapper.findByName(name) != null;
    }

    @Transactional
    public String ensureRegistered(String name) {
        String normalized = normalize(name);
        Category existing = mapper.findByNameForUpdate(normalized);
        if (existing != null) return existing.getName();
        Category category = new Category();
        category.setName(normalized);
        try {
            mapper.insert(category);
        } catch (DuplicateKeyException e) {
            // Another request inserted the same name first.
        }
        return normalized;
    }

    @Transactional
    public Category create(String name) {
        String normalized = normalize(name);
        if (mapper.findByName(normalized) != null) {
            throw new IllegalStateException("同じ名前のジャンルがあります");
        }
        Category category = new Category();
        category.setName(normalized);
        try {
            mapper.insert(category);
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("同じ名前のジャンルがあります", e);
        }
        return category;
    }

    @Transactional
    public Category rename(Long id, String name) {
        String normalized = normalize(name);
        Category category = mapper.findByIdForUpdate(id);
        if (category == null) throw new NoSuchElementException("ジャンルが見つかりません");
        if (category.getName().equals(normalized)) return category;
        Category duplicate = mapper.findByName(normalized);
        if (duplicate != null && !duplicate.getId().equals(id)) {
            throw new IllegalStateException("同じ名前のジャンルがあります");
        }
        try {
            mapper.rename(id, normalized);
        } catch (DuplicateKeyException e) {
            throw new IllegalStateException("同じ名前のジャンルがあります", e);
        }
        mapper.renameTodos(category.getName(), normalized);
        category.setName(normalized);
        return category;
    }

    @Transactional
    public void delete(Long id) {
        Category category = mapper.findByIdForUpdate(id);
        if (category == null) throw new NoSuchElementException("ジャンルが見つかりません");
        if (mapper.countTodos(category.getName()) != 0 || mapper.deleteIfUnused(id) == 0) {
            throw new IllegalStateException("登録済みTodoが使用しているジャンルは削除できません");
        }
    }

    private String normalize(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("ジャンル名を入力してください");
        }
        String normalized = name.strip();
        if (normalized.length() > 255) {
            throw new IllegalArgumentException("ジャンル名は255文字以内で入力してください");
        }
        return normalized;
    }
}
