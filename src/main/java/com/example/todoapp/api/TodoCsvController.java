package com.example.todoapp.api;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.todoapp.Todo;
import com.example.todoapp.TodoService;

@RestController
public class TodoCsvController {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final TodoService todoService;

    public TodoCsvController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping("/api/todos.csv")
    public ResponseEntity<byte[]> download(
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "category", defaultValue = "") String category,
            @RequestParam(name = "order", defaultValue = "asc") String order,
            @RequestParam(name = "showCompleted", defaultValue = "false") boolean showCompleted,
            @RequestParam(name = "trash", defaultValue = "0") String trash) {
        boolean viewingTrash = "1".equals(trash);
        String sortOrder = "desc".equals(order) ? "desc" : "asc";
        List<Todo> todos = todoService.searchForCsv(keyword, category, sortOrder, showCompleted, viewingTrash);

        StringBuilder csv = new StringBuilder("\uFEFF");
        appendRow(csv, "やること", "ジャンル", "優先度", "期限", "状態", "メモ");
        for (Todo todo : todos) {
            String title = Boolean.TRUE.equals(todo.getPinned()) ? "📌 " + todo.getTitle() : todo.getTitle();
            appendRow(csv, title, todo.getCategory(), priorityLabel(todo.getPriority()),
                    todo.getDueDate() == null ? "" : todo.getDueDate().toString(),
                    statusLabel(todo, viewingTrash), todo.getDetail());
        }

        byte[] body = csv.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"todos.csv\"")
                .body(body);
    }

    private static String priorityLabel(Integer priority) {
        if (priority == null) {
            return "";
        }
        return switch (priority) {
            case 1 -> "高";
            case 2 -> "中";
            case 3 -> "低";
            default -> "";
        };
    }

    private static String statusLabel(Todo todo, boolean viewingTrash) {
        if (viewingTrash) {
            return withDate("削除済み", todo.getDeletedAt());
        }
        return Boolean.TRUE.equals(todo.getCompleted())
                ? withDate("完了", todo.getCompletedAt())
                : "未完了";
    }

    private static String withDate(String label, LocalDateTime dateTime) {
        return dateTime == null ? label : label + " " + DATE_TIME.format(dateTime);
    }

    private static void appendRow(StringBuilder csv, String... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            String value = values[i] == null ? "" : values[i];
            if (startsLikeFormula(value)) {
                value = "'" + value;
            }
            csv.append('"').append(value.replace("\"", "\"\"")).append('"');
        }
        csv.append("\r\n");
    }

    private static boolean startsLikeFormula(String value) {
        int index = 0;
        while (index < value.length()) {
            char character = value.charAt(index);
            if (!Character.isWhitespace(character) && !Character.isSpaceChar(character)
                    && !Character.isISOControl(character)) {
                return character == '=' || character == '+' || character == '-'
                        || character == '@';
            }
            index++;
        }
        return false;
    }
}
