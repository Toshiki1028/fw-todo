package com.example.todoapp.mcp;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import com.example.todoapp.Todo;
import com.example.todoapp.TodoService;
import com.example.todoapp.api.HolidayClient;
import com.example.todoapp.api.TodoDto;

@Component
public class TodoTools {

    private final TodoService todoService;
    private final HolidayClient holidayClient;
    private static final Set<String> CATEGORIES = Set.of(
            "デザイン", "マーケティング", "プログラミング", "資格", "就職活動");

    public TodoTools(TodoService todoService, HolidayClient holidayClient) {
        this.todoService = todoService;
        this.holidayClient = holidayClient;
    }

    @McpTool(name = "list_todos", description = "やることの一覧を返す（期間・ジャンルで絞れる）")
    public List<TodoDto> listTodos(
            @McpToolParam(description = "キーワード", required = false) String keyword,
            @McpToolParam(description = "ジャンル", required = false) String category,
            @McpToolParam(description = "開始日（yyyy-MM-dd）", required = false) String from,
            @McpToolParam(description = "終了日（yyyy-MM-dd）", required = false) String to) {
        LocalDate fromDate = from == null ? null : LocalDate.parse(from);
        LocalDate toDate = to == null ? null : LocalDate.parse(to);
        return todoService.search(keyword, category, "asc", fromDate, toDate).stream()
                .map(TodoDto::from)
                .toList();
    }

    @McpTool(name = "get_todo", description = "やることを1件返す")
    public TodoDto getTodo(@McpToolParam(description = "やることのID", required = true) Long id) {
        Todo todo = todoService.findById(id);
        return todo == null ? null : TodoDto.from(todo);
    }

    @McpTool(name = "add_todo", description = "やることを1件足す")
    public TodoDto addTodo(
            @McpToolParam(description = "やること", required = true) String title,
            @McpToolParam(description = "メモ", required = false) String detail,
            @McpToolParam(description = "ジャンル", required = true) String category,
            @McpToolParam(description = "優先度（1: 高、2: 中、3: 低）", required = true) Integer priority,
            @McpToolParam(description = "期限（yyyy-MM-dd）", required = false) String dueDate) {
        if (title == null || title.isBlank() || title.length() > 255) {
            throw new IllegalArgumentException("やることは1〜255文字で入力してください");
        }
        if (detail != null && detail.length() > 255) {
            throw new IllegalArgumentException("メモは255文字以内で入力してください");
        }
        if (category == null || !CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("ジャンルは指定された5種類から選んでください");
        }
        if (priority == null || priority < 1 || priority > 3) {
            throw new IllegalArgumentException("優先度は1〜3で入力してください");
        }

        Todo todo = new Todo();
        todo.setTitle(title);
        todo.setDetail(detail);
        todo.setCategory(category);
        todo.setPriority(priority);
        todo.setDueDate(dueDate == null || dueDate.isBlank() ? null : LocalDate.parse(dueDate));
        todo.setCompleted(false);
        todoService.create(todo);
        return TodoDto.from(todoService.findById(todo.getId()));
    }

    @McpTool(name = "update_todo", description = "やることを1件直す（期限を変えるのもこれ）")
    public TodoDto updateTodo(
            @McpToolParam(description = "やることのID", required = true) Long id,
            @McpToolParam(description = "やること", required = false) String title,
            @McpToolParam(description = "メモ（空文字で消去）", required = false) String detail,
            @McpToolParam(description = "ジャンル", required = false) String category,
            @McpToolParam(description = "優先度（1: 高、2: 中、3: 低）", required = false) Integer priority,
            @McpToolParam(description = "期限（yyyy-MM-dd、空文字で消去）", required = false) String dueDate) {
        Todo todo = todoService.findById(id);
        if (todo == null) {
            return null;
        }
        if (title != null) {
            if (title.isBlank() || title.length() > 255) {
                throw new IllegalArgumentException("やることは1〜255文字で入力してください");
            }
            todo.setTitle(title);
        }
        if (detail != null) {
            if (detail.length() > 255) {
                throw new IllegalArgumentException("メモは255文字以内で入力してください");
            }
            todo.setDetail(detail.isEmpty() ? null : detail);
        }
        if (category != null) {
            if (!CATEGORIES.contains(category)) {
                throw new IllegalArgumentException("ジャンルは指定された5種類から選んでください");
            }
            todo.setCategory(category);
        }
        if (priority != null) {
            if (priority < 1 || priority > 3) {
                throw new IllegalArgumentException("優先度は1〜3で入力してください");
            }
            todo.setPriority(priority);
        }
        if (dueDate != null) {
            todo.setDueDate(dueDate.isBlank() ? null : LocalDate.parse(dueDate));
        }
        todoService.update(todo);
        return TodoDto.from(todoService.findById(id));
    }

    @McpTool(name = "complete_todo", description = "やることを完了にする")
    public TodoDto completeTodo(@McpToolParam(description = "やることのID", required = true) Long id) {
        Todo todo = todoService.findById(id);
        if (todo == null) {
            return null;
        }
        todo.setCompleted(true);
        todoService.update(todo);
        return TodoDto.from(todoService.findById(id));
    }

    @McpTool(name = "delete_todo", description = "やることを1件消す")
    public boolean deleteTodo(@McpToolParam(description = "やることのID", required = true) Long id) {
        return todoService.delete(id) > 0;
    }

    @McpTool(name = "find_free_days", description = "期間の中で、期限のやることが無く、土日でも祝日でもない「空いている日」を返す。やることの期限を動かす先を決めるのに使う")
    public List<String> findFreeDays(
            @McpToolParam(description = "開始日（yyyy-MM-dd）", required = true) String from,
            @McpToolParam(description = "終了日（yyyy-MM-dd）", required = true) String to) {
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("開始日は終了日以前にしてください");
        }

        Set<LocalDate> busyDays = todoService.search(null, null, "asc", fromDate, toDate).stream()
                .map(Todo::getDueDate)
                .filter(date -> date != null)
                .collect(Collectors.toSet());
        HolidayClient.HolidayFetchResult holidayResult = holidayClient.fetchHolidays();
        if (holidayResult.unavailable() || holidayResult.holidays() == null) {
            throw new IllegalStateException("祝日を取得できませんでした");
        }
        Set<String> holidays = holidayResult.holidays().keySet();

        List<String> freeDays = new ArrayList<>();
        for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            if (busyDays.contains(date)) {
                continue;
            }
            if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                continue;
            }
            if (holidays.contains(date.toString())) {
                continue;
            }
            freeDays.add(date.toString());
        }
        return freeDays;
    }
}
