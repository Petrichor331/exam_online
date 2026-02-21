package com.example.common.vo;

import lombok.Data;

import java.util.List;

/**
 * 成绩详情VO
 * 用于改卷页面展示答卷详情
 */
@Data
public class ScoreDetailVO {
    private Integer scoreId;           // 考试记录ID
    private Integer studentId;         // 学生ID
    private Integer paperId;           // 试卷ID
    private String studentName;        // 学生姓名
    private String paperName;          // 试卷名称
    private String courseName;         // 课程名称
    private String submitTime;         // 提交时间
    private String status;             // grading/finished
    private Integer totalScore;        // 总分
    private List<AnswerDetail> answers; // 答题详情列表
    
    /**
     * 答题详情
     */
    @Data
    public static class AnswerDetail {
        private Integer answerId;           // 答案ID
        private Integer questionId;         // 题目ID
        private String questionName;        // 题目内容
        private Integer questionTypeId;     // 题型ID（1单选 2多选 3判断 4填空 5简答）
        private String studentAnswer;       // 学生答案
        private String correctAnswer;       // 标准答案（选择题）
        private String referenceAnswer;     // 参考答案（主观题）
        private Integer maxScore;           // 满分
        private Integer aiScore;            // AI评分
        private Integer finalScore;         // 最终分数
        private String gradingStatus;       // pending/ai_graded/graded
        private String remark;              // 评语
    }
}
