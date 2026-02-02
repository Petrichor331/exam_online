package com.example.common.dto;

import lombok.Data;

import java.util.List;

@Data
public class QuestionAddDTO {
    private Integer id;//题目ID（编辑时需要）
    private String name;//题干
    private Integer courseId;//所属课程
    private Integer typeId;//题型
    private Integer score;//分值
    private String referenceAnswer;//标准答案
    // 选择题选项（非选择题可为空）
    private List<QuestionOptionDTO> options;

    public String getReferenceAnswer() {
        return referenceAnswer;
    }

    public void setReferenceAnswer(String referenceAnswer) {
        this.referenceAnswer = referenceAnswer;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(Integer typeId) {
        this.typeId = typeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }
}
