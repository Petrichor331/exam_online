package com.example.common.dto;


//出卷时前端传的那些参数,只负责试卷的基本信息，不涉及题目内容
public class TestPaperAddDTO {
    private Integer courseId;//前端选课程，传id
    private String paperName;
    private String startTime;
    private String endTime;
    private Integer duration;   // 考试时长
    private String generateType; // 出卷方式

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getPaperName() {
        return paperName;
    }

    public void setPaperName(String paperName) {
        this.paperName = paperName;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getGenerateType() {
        return generateType;
    }

    public void setGenerateType(String generateType) {
        this.generateType = generateType;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }
}
