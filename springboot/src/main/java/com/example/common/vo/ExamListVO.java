package com.example.common.vo;

import lombok.Data;

/**
 * 考试列表VO
 */
@Data
public class ExamListVO {
    /**
     * 考试记录ID (score表ID)
     */
    private Integer id;
    
    /**
     * 试卷ID
     */
    private Integer paperId;
    
    /**
     * 试卷名称
     */
    private String paperName;
    
    /**
     * 题目数量
     */
    private Integer questionCount;
    
    /**
     * 试卷总分
     */
    private Integer totalScore;
    
    /**
     * 教师名称
     */
    private String teacherName;
    
    /**
     * 开始时间
     */
    private String startTime;
    
    /**
     * 结束时间
     */
    private String endTime;
    
    /**
     * 状态: pending-待考试, ongoing-进行中, waiting-待评分, finished-已完成
     */
    private String status;
    
    /**
     * 考试成绩
     */
    private Integer examScore;
    
    /**
     * score表ID，用于查看答卷
     */
    private Integer scoreId;
}
