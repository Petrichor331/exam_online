package com.example.common.vo;

import lombok.Data;

import java.util.List;

/**
 * 首页数据VO
 */
@Data
public class HomeDataVO {
    /**
     * 进行中的考试
     */
    private List<OngoingExamVO> ongoingExams;
    
    /**
     * 待考科目
     */
    private List<UpcomingExamVO> upcomingExams;
    
    /**
     * 最近成绩
     */
    private List<RecentScoreVO> recentScores;
    
    /**
     * 进行中的考试VO
     */
    @Data
    public static class OngoingExamVO {
        private Integer id;              // score表ID
        private Integer paperId;         // 试卷ID
        private String name;             // 试卷名称
        private Integer questionCount;   // 题目数量
        private Integer totalScore;      // 总分
        private String teacherName;      // 教师名称
        private Integer remainingSeconds; // 剩余秒数
    }
    
    /**
     * 待考科目VO
     */
    @Data
    public static class UpcomingExamVO {
        private Integer id;              // 试卷ID
        private String name;             // 试卷名称
        private String startTime;        // 开始时间
    }
    
    /**
     * 最近成绩VO
     */
    @Data
    public static class RecentScoreVO {
        private Integer id;              // score表ID
        private String examName;         // 考试名称
        private Double score;            // 最终得分
        private Integer totalScore;      // 总分
        private String submitTime;       // 提交时间
    }
}