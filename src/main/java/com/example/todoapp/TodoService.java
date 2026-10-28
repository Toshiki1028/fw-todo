package com.example.todoapp;

import java.util.List;
import java.time.LocalDate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TodoService {

    private final TodoMapper todoMapper;

    public TodoService(TodoMapper todoMapper) {
        this.todoMapper = todoMapper;
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

    public int create(Todo todo) {
        int result = todoMapper.insert(todo);
        log.info("Todo created");
        return result;
    }

    public int update(Todo todo) {
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
}
