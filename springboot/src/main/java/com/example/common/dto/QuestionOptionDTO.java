package com.example.common.dto;


import lombok.Data;

@Data
public class QuestionOptionDTO {
    private String optionLabel;// A/B/C/D
    private String optionContent; //选项内容

    public String getOptionLabel() {
        return optionLabel;
    }

    public void setOptionLabel(String optionLabel) {
        this.optionLabel = optionLabel;
    }

    public String getOptionContent() {
        return optionContent;
    }

    public void setOptionContent(String optionContent) {
        this.optionContent = optionContent;
    }
}
