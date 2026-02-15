package com.example.mapper;

import com.example.entity.StudentCourse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 学生选课Mapper
 */
@Mapper
public interface StudentCourseMapper {
    
    /**
     * 插入选课记录
     */
    int insert(StudentCourse studentCourse);
    
    /**
     * 根据学生ID查询所有选课
     */
    List<StudentCourse> selectByStudentId(@Param("studentId") Integer studentId);
    
    /**
     * 查询学生是否已选某课程
     */
    int countByStudentAndCourse(@Param("studentId") Integer studentId, 
                                 @Param("courseId") Integer courseId);
    
    /**
     * 查询学生选的所有课程ID
     */
    List<Integer> selectCourseIdsByStudentId(@Param("studentId") Integer studentId);
    
    /**
     * 删除选课记录
     */
    int deleteByStudentAndCourse(@Param("studentId") Integer studentId, 
                                  @Param("courseId") Integer courseId);
    
    /**
     * 查询某课程的所有学生
     */
    List<StudentCourse> selectByCourseId(@Param("courseId") Integer courseId);
}