package com.example.common.dto;

import lombok.Data;

/**
 * 成绩查询条件DTO
 */
@Data
public class ScoreQueryDTO {
    private String paperName;          // 试卷名称（模糊查询）
    private String studentName;        // 学生姓名（模糊查询）
    private String status;             // 状态筛选：grading/finished
    private Integer teacherId;         // 教师ID（权限控制用）
    private Integer pageNum;           // 页码
    private Integer pageSize;          // 每页条数
}
