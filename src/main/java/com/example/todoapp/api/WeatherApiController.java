package com.example.todoapp.api;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WeatherApiController {

    private final WeatherClient weatherClient;

    public WeatherApiController(WeatherClient weatherClient) {
        this.weatherClient = weatherClient;
    }

    @GetMapping("/api/weather")
    public ResponseEntity<Map<String, String>> weather(
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        WeatherClient.WeatherFetchResult result = weatherClient.fetchWeather();
        HttpHeaders headers = new HttpHeaders();
        if (result.unavailable()) {
            headers.set("X-Weather-Unavailable", "true");
        }

        Map<String, String> filteredWeather = new LinkedHashMap<>();
        result.weather().forEach((date, description) -> {
            if ((from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to))) {
                filteredWeather.put(date.toString(), description);
            }
        });
        return ResponseEntity.ok().headers(headers).body(filteredWeather);
    }
}
