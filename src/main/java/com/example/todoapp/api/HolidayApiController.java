package com.example.todoapp.api;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/holidays")
public class HolidayApiController {

    private final HolidayClient holidayClient;

    public HolidayApiController(HolidayClient holidayClient) {
        this.holidayClient = holidayClient;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> holidays(
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        HolidayClient.HolidayFetchResult result = holidayClient.fetchHolidays();
        Map<String, String> holidays = result.holidays();
        HttpHeaders headers = new HttpHeaders();
        if (result.unavailable()) {
            headers.set("X-Holidays-Unavailable", "true");
        }
        if (from == null && to == null) {
            return ResponseEntity.ok().headers(headers).body(holidays);
        }

        Map<String, String> filteredHolidays = new LinkedHashMap<>();
        holidays.forEach((date, name) -> {
            LocalDate holidayDate = LocalDate.parse(date);
            if ((from == null || !holidayDate.isBefore(from))
                    && (to == null || !holidayDate.isAfter(to))) {
                filteredHolidays.put(date, name);
            }
        });
        return ResponseEntity.ok().headers(headers).body(filteredHolidays);
    }
}
