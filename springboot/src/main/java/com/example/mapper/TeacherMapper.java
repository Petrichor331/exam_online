package com.example.mapper;

import com.example.entity.Teacher;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TeacherMapper {


    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Teacher teacher);

    @Select("select * from `teacher` where username=#{username}")
    Teacher selectByUsername(String username);

    List<Teacher> selectAll(Teacher teacher);

    void updateById(Teacher teacher);

    void deleteById(Integer id);

    @Select("select * from `teacher` where id=#{id}")
    Teacher selectById(Integer id);

    @Select("select * from `teacher` where email=#{email}")
    Teacher selectByEmail(String email);
}
