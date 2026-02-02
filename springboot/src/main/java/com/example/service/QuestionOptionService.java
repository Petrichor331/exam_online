package com.example.service;

import com.example.entity.QuestionOption;
import com.example.mapper.QuestionOptionMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionOptionService {
    @Resource
    private QuestionOptionMapper questionOptionMapper;

    public List<QuestionOption> selectByQuestionId(Integer questionId) {
        return questionOptionMapper.selectByQuestionId(questionId);
    }
}