package com.example.service;

import com.example.entity.Account;
import com.example.entity.Course;
import com.example.entity.StudentCourse;
import com.example.mapper.CourseMapper;
import com.example.mapper.StudentCourseMapper;
import com.example.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private CourseMapper courseMapper;

    /**
     * 学生选课
     */
    @Transactional
    public void selectCourse(Integer studentId, Integer courseId,String code) {
        //先验证课程码是否正确
        Course course = courseMapper.selectById(courseId);
        if(course!=null && course.getCode() != null){
            if(!course.getCode().equals(code)){
                throw new RuntimeException("课程码错误");
            }
        }
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