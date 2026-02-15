package com.example.service;

import com.example.entity.Account;
import com.example.entity.StudentCourse;
import com.example.mapper.StudentCourseMapper;
import com.example.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 选课服务
 */
@Service
public class StudentCourseService {
    
    @Resource
    private StudentCourseMapper studentCourseMapper;
    
    /**
     * 学生选课
     */
    @Transactional
    public void selectCourse(Integer studentId, Integer courseId) {
        // 检查是否已选
        int count = studentCourseMapper.countByStudentAndCourse(studentId, courseId);
        if (count > 0) {
            throw new RuntimeException("您已选过该课程");
        }
        
        StudentCourse sc = new StudentCourse();
        sc.setStudentId(studentId);
        sc.setCourseId(courseId);
        studentCourseMapper.insert(sc);
    }
    
    /**
     * 退课
     */
    @Transactional
    public void dropCourse(Integer studentId, Integer courseId) {
        int count = studentCourseMapper.countByStudentAndCourse(studentId, courseId);
        if (count == 0) {
            throw new RuntimeException("您未选该课程");
        }
        
        studentCourseMapper.deleteByStudentAndCourse(studentId, courseId);
    }
    
    /**
     * 查询学生的所有选课
     */
    public List<StudentCourse> getMyCourses(Integer studentId) {
        return studentCourseMapper.selectByStudentId(studentId);
    }
    
    /**
     * 检查是否已选某课程
     */
    public boolean hasSelected(Integer studentId, Integer courseId) {
        return studentCourseMapper.countByStudentAndCourse(studentId, courseId) > 0;
    }
}