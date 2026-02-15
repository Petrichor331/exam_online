package com.example.common.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量保存答案请求DTO
 */
@Data
public class SaveAnswerDTO {
    /**
     * 试卷ID
     */
    private Integer paperId;
    
    /**
     * 答案列表
     */
    private List<AnswerItem> answers;
    
    @Data
    public static class AnswerItem {
        /**
         * 题目ID
         */
        private Integer questionId;
        
        /**
         * 答案内容
         * 客观题：选项（如 "A" 或 "A,B,C"）
         * 主观题：文本内容
         */
        private String answer;
    }
}