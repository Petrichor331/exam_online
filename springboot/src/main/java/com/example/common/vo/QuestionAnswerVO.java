package com.example.common.vo;

import lombok.Data;

/**
 * 题目答题详情VO
 * 嵌套在ScoreDetailVO中使用
 */
@Data
public class QuestionAnswerVO {
    // 题目信息
    private Integer questionId;        // 题目ID
    private String questionName;       // 题干内容
    private Integer typeId;            // 题型ID（1单选/2多选/3判断/4填空/5简答）
    private String typeName;           // 题型名称
    private Integer maxScore;          // 该题满分
    
    // 答题信息
    private String studentAnswer;      // 学生答案
    private String correctAnswer;      // 标准答案
    private Integer aiScore;           // AI评分（简答题有）
    private Integer finalScore;        // 最终得分
    private String gradingStatus;      // pending/graded/ai_graded
    
    // 控制字段
    private boolean canEdit;           // 是否可以修改分数（填空题和简答题可修改）
    private String comment;            // 评语（可选）
}
