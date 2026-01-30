package com.example.common.vo;


import lombok.Data;

@Data
//我们想在列表展示的题目信息，搞成一个VO
public class QuestionListVO {
    private Integer id;
    private String name;
    private String courseName;
    private String typeName;
    private String optionsText;
    private String referenceAnswer;
    private String score;

}
