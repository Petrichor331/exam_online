package com.example.common.dto;

import lombok.Data;

import java.util.List;

@Data
public class QuestionAddDTO {
    private Integer id;//题目ID（编辑时需要）
    private String name;//题干
    private Integer courseId;//所属课程
    private Integer typeId;//题型
    private Integer score;//分值
    private Integer difficulty;//难度
    private String knowledgePoint;//知识点
    private Object referenceAnswer;//标准答案（单选/判断/填空/简答为String，多选为List<String>）
    // 选择题选项（非选择题可为空）
    private List<QuestionOptionDTO> options;

    /**
     * 获取标准答案的字符串格式（用于保存到数据库）
     * 多选题会将数组转换为 "A,B,C" 格式
     */
    public String getReferenceAnswerString() {
        if (referenceAnswer == null) {
            return "";
        }
        if (referenceAnswer instanceof List) {
            // 多选题，将列表转换为逗号分隔的字符串
            List<?> list = (List<?>) referenceAnswer;
            return String.join(",", list.stream().map(Object::toString).toList());
        }
        return referenceAnswer.toString();
    }

    public Object getReferenceAnswer() {
        return referenceAnswer;
    }

    public void setReferenceAnswer(Object referenceAnswer) {
        this.referenceAnswer = referenceAnswer;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
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

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }

    public String getKnowledgePoint() {
        return knowledgePoint;
    }

    public void setKnowledgePoint(String knowledgePoint) {
        this.knowledgePoint = knowledgePoint;
    }
}
