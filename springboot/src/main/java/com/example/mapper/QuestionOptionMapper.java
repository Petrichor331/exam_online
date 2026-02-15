package com.example.mapper;

import com.example.common.dto.QuestionOptionDTO;
import com.example.entity.QuestionOption;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface   QuestionOptionMapper {

    int insert(QuestionOption questionOption);

    void insertBatch(List<QuestionOption> options);

    void deleteByQuestionId(Integer questionId);

    List<QuestionOption> selectByQuestionId(Integer questionId);

    /**
     * 查询考试用选项（不包含正确答案）
     */
    List<QuestionOptionDTO> selectOptionsForExam(Integer questionId);
}
