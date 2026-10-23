package com.example.todoapp.api;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.todoapp.Todo;
import com.example.todoapp.TodoService;

@RestController
@RequestMapping("/api/todos")
public class TodoApiController {

    private final TodoService todoService;
    private final TodoApiMapper todoApiMapper;

    public TodoApiController(TodoService todoService, TodoApiMapper todoApiMapper) {
        this.todoService = todoService;
        this.todoApiMapper = todoApiMapper;
    }

    @GetMapping
    public List<TodoDto> list(
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "category", defaultValue = "") String category,
            @RequestParam(name = "order", defaultValue = "asc") String order) {
        List<Todo> searchResults = todoService.search(keyword, category, order);
        if (searchResults.isEmpty()) {
            return List.of();
        }

        List<Long> ids = searchResults.stream()
                .map(Todo::getId)
                .toList();
        Map<Long, Todo> todosById = todoApiMapper.findByIds(ids).stream()
                .collect(Collectors.toMap(Todo::getId, Function.identity()));

        return searchResults.stream()
                .map(todo -> todosById.get(todo.getId()))
                .filter(todo -> todo != null)
                .map(TodoDto::from)
                .toList();
    }
}
