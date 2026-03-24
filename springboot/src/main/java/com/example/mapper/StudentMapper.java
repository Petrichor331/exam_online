package com.example.mapper;

import com.example.entity.Student;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Options;

import java.util.List;

public interface StudentMapper {


    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Student student);

    @Select("select * from `student` where username=#{username}")
    Student selectByUsername(String username);

    List<Student> selectAll(Student student);

    void updateById(Student student);

    void deleteById(Integer id);

    @Select("select * from `student` where id=#{id}")
    Student selectById(Integer id);

    @Select("select * from `student` where email=#{email}")
    Student selectByEmail(String email);
}
