package com.example.todoapp.api;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class WeatherClient {

    private final RestClient restClient;

    public WeatherClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(5000);
        this.restClient = RestClient.builder()
                .baseUrl("https://api.open-meteo.com")
                .requestFactory(requestFactory)
                .build();
    }

    public WeatherFetchResult fetchWeather() {
        try {
            Forecast forecast = restClient.get()
                    .uri(uri -> uri.path("/v1/forecast")
                            .queryParam("latitude", "35.6895")
                            .queryParam("longitude", "139.6917")
                            .queryParam("daily", "weather_code")
                            .queryParam("timezone", "Asia/Tokyo")
                            .queryParam("past_days", 7)
                            .queryParam("forecast_days", 16)
                            .build())
                    .retrieve()
                    .body(Forecast.class);
            if (forecast == null || forecast.daily() == null
                    || forecast.daily().time() == null || forecast.daily().weatherCode() == null) {
                return new WeatherFetchResult(Map.of(), true);
            }

            Map<LocalDate, String> weather = new LinkedHashMap<>();
            List<String> dates = forecast.daily().time();
            List<Integer> codes = forecast.daily().weatherCode();
            for (int i = 0; i < Math.min(dates.size(), codes.size()); i++) {
                if (dates.get(i) == null || codes.get(i) == null) continue;
                try {
                    weather.put(LocalDate.parse(dates.get(i)), description(codes.get(i)));
                } catch (DateTimeParseException ignored) {
                    // Ignore a malformed date from the weather provider.
                }
            }
            return new WeatherFetchResult(weather, false);
        } catch (RestClientException exception) {
            return new WeatherFetchResult(Map.of(), true);
        }
    }

    private static String description(int code) {
        return switch (code) {
            case 0 -> "晴れ";
            case 1, 2 -> "晴れ時々曇り";
            case 3 -> "曇り";
            case 45, 48 -> "霧";
            case 51, 53, 55, 56, 57 -> "小雨";
            case 61, 63, 65, 66, 67, 80, 81, 82 -> "雨";
            case 71, 73, 75, 77, 85, 86 -> "雪";
            case 95, 96, 99 -> "雷雨";
            default -> "天気不明";
        };
    }

    private record Forecast(Daily daily) {
    }

    private record Daily(List<String> time, @JsonProperty("weather_code") List<Integer> weatherCode) {
    }

    public record WeatherFetchResult(Map<LocalDate, String> weather, boolean unavailable) {
    }
}
