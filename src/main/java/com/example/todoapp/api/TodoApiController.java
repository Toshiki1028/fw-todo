package com.example.todoapp.api;

import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.validation.BindingResult;

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
            @RequestParam(name = "order", defaultValue = "asc") String order,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<Todo> searchResults = todoService.search(keyword, category, order, from, to);
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

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        Todo todo = todoService.findById(id);
        if (todo == null) {
            return notFound();
        }
        return ResponseEntity.ok(TodoDto.from(todo));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody TodoRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return badRequest(bindingResult);
        }
        Todo todo = request.toTodo();
        todoService.create(todo);
        Todo createdTodo = todoService.findById(todo.getId());
        if (createdTodo == null) {
            throw new IllegalStateException("Created Todo could not be loaded: id=" + todo.getId());
        }
        URI location = URI.create("/api/todos/" + createdTodo.getId());
        return ResponseEntity.created(location).body(TodoDto.from(createdTodo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> replace(
            @PathVariable Long id,
            @Valid @RequestBody TodoRequest request,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return badRequest(bindingResult);
        }
        if (todoService.findById(id) == null) {
            return notFound();
        }
        Todo todo = request.toTodo();
        todo.setId(id);
        todoService.update(todo);

        Todo updatedTodo = todoService.findById(id);
        if (updatedTodo == null) {
            return notFound();
        }
        return ResponseEntity.ok(TodoDto.from(updatedTodo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (todoService.findById(id) == null) {
            return notFound();
        }
        todoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Map<String, Object>> notFound() {
        String instance = ServletUriComponentsBuilder.fromCurrentRequest()
                .build()
                .getPath();
        Map<String, Object> body = Map.of(
                "type", "about:blank",
                "title", "Not Found",
                "status", HttpStatus.NOT_FOUND.value(),
                "detail", "指定されたやることは見つかりませんでした",
                "instance", instance);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    private ResponseEntity<Map<String, Object>> badRequest(BindingResult bindingResult) {
        List<Map<String, String>> errors = new ArrayList<>();
        bindingResult.getFieldErrors().forEach(error -> errors.add(Map.of(
                "field", error.getField(),
                "message", error.getDefaultMessage())));

        String instance = ServletUriComponentsBuilder.fromCurrentRequest()
                .build()
                .getPath();
        Map<String, Object> body = Map.of(
                "type", "about:blank",
                "title", "Bad Request",
                "status", HttpStatus.BAD_REQUEST.value(),
                "detail", "入力に誤りがあります",
                "instance", instance,
                "errors", errors);
        return ResponseEntity.badRequest().body(body);
    }
}
