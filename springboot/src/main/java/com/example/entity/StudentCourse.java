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
    
    // 学生信息
    private String username;
    private String avatar;
    private String name;
    private String selectTime;
    
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
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getAvatar() {
        return avatar;
    }
    
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getSelectTime() {
        return selectTime;
    }
    
    public void setSelectTime(String selectTime) {
        this.selectTime = selectTime;
    }
}