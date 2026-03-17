package com.example.service;

import com.example.entity.Account;
import com.example.entity.Course;
import com.example.entity.Student;
import com.example.entity.StudentCourse;
import com.example.mapper.CourseMapper;
import com.example.mapper.StudentCourseMapper;
import com.example.mapper.StudentMapper;
import com.example.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    @Autowired
    private StudentMapper studentMapper;

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
    
    /**
     * 查询课程的所有学生（教师用）
     */
    public List<StudentCourse> getCourseStudents(Integer courseId) {
        List<StudentCourse> list = studentCourseMapper.selectByCourseId(courseId);
        
        // 查询课程名称
        Course course = courseMapper.selectById(courseId);
        String courseName = course != null ? course.getName() : "";
        
        // 填充学生信息
        for (StudentCourse sc : list) {
            Student student = studentMapper.selectById(sc.getStudentId());
            if (student != null) {
                sc.setUsername(student.getUsername());
                sc.setName(student.getName());
                sc.setAvatar(student.getAvatar());
            }
            sc.setCourseName(courseName);
            // 格式化选课时间
            if (sc.getCreateTime() != null) {
                sc.setSelectTime(sc.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
        }
        
        return list;
    }
    
    /**
     * 移除学生（教师操作）
     */
    @Transactional
    public void removeStudent(Integer courseId, Integer studentId, String teacherName) {
        int count = studentCourseMapper.countByStudentAndCourse(studentId, courseId);
        if (count == 0) {
            throw new RuntimeException("该学生未选此课程");
        }
        
        // 获取课程名称
        Course course = courseMapper.selectById(courseId);
        String courseName = course != null ? course.getName() : "未知课程";
        
        // 删除选课记录
        studentCourseMapper.deleteByStudentAndCourse(studentId, courseId);
        
        // 发送通知给学生（这里暂时记录日志，实际可以用消息队列或邮件等方式）
        // TODO: 发送通知给学生 - 您已被 XX 老师从 XX 课程中移除
        System.out.println("通知：学生ID " + studentId + " 已被教师 " + teacherName + " 从课程 " + courseName + " 中移除");
    }
}