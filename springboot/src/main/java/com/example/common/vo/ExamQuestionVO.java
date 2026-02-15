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
    private List<QuestionOptionDTO> options;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public List<QuestionOptionDTO> getOptions() {
        return options;
    }

    public void setOptions(List<QuestionOptionDTO> options) {
        this.options = options;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
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
}
