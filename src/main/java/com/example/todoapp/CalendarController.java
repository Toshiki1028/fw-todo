package com.example.todoapp;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CalendarController {

    @GetMapping("/calendar")
    public String calendar(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month,
            Model model) {
        YearMonth currentMonth = YearMonth.now();
        YearMonth displayedMonth = YearMonth.of(
                year == null ? currentMonth.getYear() : year,
                month == null ? currentMonth.getMonthValue() : month);
        LocalDate firstDay = displayedMonth.atDay(1);
        LocalDate lastDay = displayedMonth.atEndOfMonth();

        int leadingEmptyCells = firstDay.getDayOfWeek().getValue() % 7;
        int cellCount = ((leadingEmptyCells + displayedMonth.lengthOfMonth() + 6) / 7) * 7;
        List<List<LocalDate>> weeks = new ArrayList<>();
        for (int cell = 0; cell < cellCount; cell += 7) {
            List<LocalDate> week = new ArrayList<>();
            for (int weekday = 0; weekday < 7; weekday++) {
                int dayOfMonth = cell + weekday - leadingEmptyCells + 1;
                week.add(dayOfMonth >= 1 && dayOfMonth <= displayedMonth.lengthOfMonth()
                        ? displayedMonth.atDay(dayOfMonth) : null);
            }
            weeks.add(week);
        }

        model.addAttribute("displayedMonth", displayedMonth);
        model.addAttribute("firstDay", firstDay);
        model.addAttribute("lastDay", lastDay);
        model.addAttribute("previousMonth", displayedMonth.minusMonths(1));
        model.addAttribute("nextMonth", displayedMonth.plusMonths(1));
        model.addAttribute("weeks", weeks);
        return "calendar";
    }
}
