package com.example.todoapp.api;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.todoapp.Todo;

@Mapper
public interface TodoApiMapper {

    List<Todo> findByIds(@Param("ids") List<Long> ids);
}
