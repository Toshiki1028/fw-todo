package com.example.todoapp;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CategoryMapper {
    List<Category> findAll();
    Category findById(Long id);
    Category findByIdForUpdate(Long id);
    Category findByName(String name);
    Category findByNameForUpdate(String name);
    int insert(Category category);
    int rename(@Param("id") Long id, @Param("name") String name);
    int renameTodos(@Param("oldName") String oldName, @Param("newName") String newName);
    int deleteIfUnused(Long id);
    int countTodos(String name);
}
