package com.example.mapper;

import com.example.entity.Course;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CourseMapper {

    int insert(Course course);

    List<Course> selectAll(Course course);

    void updateById(Course course);

    void deleteById(Integer id);

    @Select("select * from `course` where id=#{id}")
    Course selectById(Integer id);
    
    /**
     * 根据教师ID查询课程列表
     */
    @Select("select * from `course` where teacher_id=#{teacherId}")
    List<Course> selectByTeacherId(Integer teacherId);
}
