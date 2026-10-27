package com.example.todoapp;

import java.util.List;
import java.time.LocalDate;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TodoMapper {

    List<Todo> search(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("order") String order,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    List<Todo> searchPage(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("order") String order,
            @Param("showCompleted") boolean showCompleted,
            @Param("trash") boolean trash,
            @Param("limit") int limit,
            @Param("offset") int offset);

    int countForPage(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("showCompleted") boolean showCompleted,
            @Param("trash") boolean trash);

    Todo findById(Long id);

    int insert(Todo todo);

    int update(Todo todo);

    int markDeleted(Long id);

    int restore(Long id);

    int togglePinned(Long id);
}
