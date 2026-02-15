package com.example.entity;

import java.time.LocalDateTime;

/**
 * 学生选课实体
 */
public class StudentCourse {
    private Integer id;
    private Integer studentId;
    private Integer courseId;
    private LocalDateTime createTime;
    
    // 关联字段
    private String courseName;
    private String courseImg;
    private String teacherName;
    
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public Integer getStudentId() {
        return studentId;
    }
    
    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }
    
    public Integer getCourseId() {
        return courseId;
    }
    
    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public String getCourseName() {
        return courseName;
    }
    
    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }
    
    public String getCourseImg() {
        return courseImg;
    }
    
    public void setCourseImg(String courseImg) {
        this.courseImg = courseImg;
    }
    
    public String getTeacherName() {
        return teacherName;
    }
    
    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }
}