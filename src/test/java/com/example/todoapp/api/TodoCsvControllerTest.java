package com.example.todoapp.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.example.todoapp.Todo;
import com.example.todoapp.TodoService;

class TodoCsvControllerTest {

    @Test
    void downloadUsesBomAndProtectsSpreadsheetCells() {
        Todo todo = new Todo();
        todo.setTitle("=SUM(1,1)");
        todo.setDetail("+cmd,\"quoted\"\nnext line");
        todo.setCategory("プログラミング");
        todo.setPriority(1);
        todo.setDueDate(LocalDate.of(2026, 10, 30));
        todo.setCompleted(false);

        TodoService service = mock(TodoService.class);
        when(service.searchForCsv("=SUM", "", "desc", false, false)).thenReturn(List.of(todo));
        ResponseEntity<byte[]> response = new TodoCsvController(service)
                .download("=SUM", "", "desc", false, "0");

        byte[] body = response.getBody();
        assertEquals("attachment; filename=\"todos.csv\"",
                response.getHeaders().getFirst("Content-Disposition"));
        assertArrayEquals(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF},
                new byte[] {body[0], body[1], body[2]});
        String csv = new String(body, StandardCharsets.UTF_8);
        assertTrue(csv.contains("\"やること\",\"ジャンル\",\"優先度\",\"期限\",\"状態\",\"メモ\"\r\n"));
        assertTrue(csv.contains("\"'=SUM(1,1)\",\"プログラミング\",\"高\",\"2026-10-30\",\"未完了\""));
        assertTrue(csv.contains("\"'+cmd,\"\"quoted\"\"\nnext line\"\r\n"));
    }
}
