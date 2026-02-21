package com.example.common.dto;

import lombok.Data;

/**
 * 教师评分DTO
 */
@Data
public class GradeDTO {
    private Integer answerId;          // 答题记录ID
    private Integer score;             // 新分数
    private String comment;            // 评语（可选）
}
