package com.example.common.vo;

import com.example.common.dto.QuestionOptionDTO;
import lombok.Data;

import java.util.List;

@Data
public class ExamQuestionVO {
    private Integer id;//题目id
    private String name;//题干
    private Integer typeId;//题型id
    private String typeName;//题型名称
    private Integer score;//分值
    private String referenceAnswer;//参考答案
    private String courseName;//课程名称
    private Integer difficulty;//难度
    private String knowledgePoint;//知识点
    private List<QuestionOptionDTO> options;
}
