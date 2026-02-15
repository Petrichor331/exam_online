package com.example.controller;

import com.example.common.Result;
import com.example.entity.Account;
import com.example.entity.StudentCourse;
import com.example.service.StudentCourseService;
import com.example.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 选课Controller
 */
@RestController
@RequestMapping("/studentCourse")
public class StudentCourseController {
    
    @Resource
    private StudentCourseService studentCourseService;
    
    /**
     * 选课
     */
    @PostMapping("/select/{courseId}")
    public Result selectCourse(@PathVariable Integer courseId) {
        try {
            Account currentUser = TokenUtils.getCurrentUser();
            if (currentUser == null || !"STUDENT".equalsIgnoreCase(currentUser.getRole())) {
                return Result.error("请先登录学生账号");
            }
            
            studentCourseService.selectCourse(currentUser.getId(), courseId);
            return Result.success("选课成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 退课
     */
    @PostMapping("/drop/{courseId}")
    public Result dropCourse(@PathVariable Integer courseId) {
        try {
            Account currentUser = TokenUtils.getCurrentUser();
            if (currentUser == null || !"STUDENT".equalsIgnoreCase(currentUser.getRole())) {
                return Result.error("请先登录学生账号");
            }
            
            studentCourseService.dropCourse(currentUser.getId(), courseId);
            return Result.success("退课成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取我的选课列表
     */
    @GetMapping("/my-courses")
    public Result getMyCourses() {
        try {
            Account currentUser = TokenUtils.getCurrentUser();
            if (currentUser == null || !"STUDENT".equalsIgnoreCase(currentUser.getRole())) {
                return Result.error("请先登录学生账号");
            }
            
            List<StudentCourse> courses = studentCourseService.getMyCourses(currentUser.getId());
            return Result.success(courses);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 检查是否已选某课程
     */
    @GetMapping("/check/{courseId}")
    public Result checkSelected(@PathVariable Integer courseId) {
        try {
            Account currentUser = TokenUtils.getCurrentUser();
            if (currentUser == null || !"STUDENT".equalsIgnoreCase(currentUser.getRole())) {
                return Result.error("请先登录学生账号");
            }
            
            boolean selected = studentCourseService.hasSelected(currentUser.getId(), courseId);
            return Result.success(selected);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}