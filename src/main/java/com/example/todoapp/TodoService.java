package com.example.todoapp;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class TodoService {

    private final TodoMapper todoMapper;
    private final CategoryService categoryService;

    public TodoService(TodoMapper todoMapper, CategoryService categoryService) {
        this.todoMapper = todoMapper;
        this.categoryService = categoryService;
    }

    public List<Todo> search(String keyword, String category, String order) {
        return search(keyword, category, order, null, null);
    }

    public List<Todo> search(String keyword, String category, String order, LocalDate from, LocalDate to) {
        return todoMapper.search(keyword, category, order, from, to);
    }

    public List<Todo> searchPage(String keyword, String category, String order,
            boolean showCompleted, boolean trash, int limit, int offset) {
        return todoMapper.searchPage(keyword, category, order, showCompleted, trash, limit, offset);
    }

    public List<Todo> searchForCsv(String keyword, String category, String order,
            boolean showCompleted, boolean trash) {
        return todoMapper.searchForCsv(keyword, category, order, showCompleted, trash);
    }

    public int countForPage(String keyword, String category, boolean showCompleted, boolean trash) {
        return todoMapper.countForPage(keyword, category, showCompleted, trash);
    }

    public Todo findById(Long id) {
        return todoMapper.findById(id);
    }

    @Transactional
    public int create(Todo todo) {
        todo.setCategory(categoryService.ensureRegistered(todo.getCategory()));
        int result = todoMapper.insert(todo);
        log.info("Todo created");
        return result;
    }

    @Transactional
    public int update(Todo todo) {
        todo.setCategory(categoryService.ensureRegistered(todo.getCategory()));
        int result = todoMapper.update(todo);
        log.info("Todo updated: id={}", todo.getId());
        return result;
    }

    public int delete(Long id) {
        int result = todoMapper.markDeleted(id);
        log.info("Todo deleted: id={}", id);
        return result;
    }

    public int restore(Long id) {
        int result = todoMapper.restore(id);
        log.info("Todo restored: id={}", id);
        return result;
    }

    public int togglePinned(Long id) {
        int result = todoMapper.togglePinned(id);
        log.info("Todo pin toggled: id={}", id);
        return result;
    }

    @Transactional
    public boolean reorderPage(List<Long> ids, String keyword, String category,
            boolean showCompleted, int page, int pageSize) {
        if (ids == null || ids.size() < 2 || ids.size() > pageSize
                || ids.stream().anyMatch(id -> id == null)
                || new HashSet<>(ids).size() != ids.size() || page < 1) {
            return false;
        }
        List<Todo> all = todoMapper.lockManualOrder();
        List<Todo> visible = todoMapper.searchPage(keyword, category, "manual",
                showCompleted, false, pageSize, (page - 1) * pageSize);
        List<Long> original = visible.stream().map(Todo::getId).toList();
        if (visible.size() != ids.size() || !new HashSet<>(original).equals(new HashSet<>(ids))) {
            return false;
        }
        Map<Long, Boolean> pinned = new HashMap<>();
        for (Todo todo : visible) {
            pinned.put(todo.getId(), Boolean.TRUE.equals(todo.getPinned()));
        }
        // A pinned row must stay in the pinned section.
        for (int i = 0; i < ids.size(); i++) {
            if (!pinned.get(ids.get(i)).equals(pinned.get(original.get(i)))) {
                return false;
            }
        }
        if (ids.equals(original)) {
            return true;
        }
        for (boolean group : new boolean[] { true, false }) {
            List<Long> oldGroup = original.stream().filter(id -> pinned.get(id) == group).toList();
            List<Long> newGroup = ids.stream().filter(id -> pinned.get(id) == group).toList();
            if (oldGroup.equals(newGroup)) {
                continue;
            }
            List<Long> global = new ArrayList<>();
            for (Todo todo : all) {
                if (Boolean.TRUE.equals(todo.getPinned()) == group) {
                    global.add(todo.getId());
                }
            }
            int next = 0;
            for (int i = 0; i < global.size(); i++) {
                if (oldGroup.contains(global.get(i))) {
                    global.set(i, newGroup.get(next++));
                }
            }
            for (int i = 0; i < global.size(); i++) {
                todoMapper.setSortPosition(global.get(i), global.size() - i);
            }
        }
        return true;
    }
}
