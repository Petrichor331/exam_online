package com.example.common.vo;


import lombok.Data;

import java.util.List;

@Data
public class StartExamVO {
    private Integer scoreId;//这场考试记录Id
    private Integer paperId;//试卷id
    private String paperName;//试卷名称
    private Integer time;//考试时长
    private Long endTime;//结束时间，用当前时间加上考试时长
    private List<ExamQuestionVO> questions;//题目列表


    public Integer getScoreId() {
        return scoreId;
    }

    public void setScoreId(Integer scoreId) {
        this.scoreId = scoreId;
    }

    public Integer getPaperId() {
        return paperId;
    }

    public void setPaperId(Integer paperId) {
        this.paperId = paperId;
    }

    public String getPaperName() {
        return paperName;
    }

    public void setPaperName(String paperName) {
        this.paperName = paperName;
    }

    public Integer getTime() {
        return time;
    }

    public void setTime(Integer time) {
        this.time = time;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public List<ExamQuestionVO> getQuestions() {
        return questions;
    }

    public void setQuestions(List<ExamQuestionVO> questions) {
        this.questions = questions;
    }
}
