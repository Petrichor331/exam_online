package com.example.service;


import cn.hutool.core.date.DateUtil;
import com.example.common.Constants;
import com.example.common.dto.QuestionAddDTO;
import com.example.common.dto.QuestionOptionDTO;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.common.vo.QuestionListVO;
import com.example.entity.Account;
import com.example.entity.Question;
import com.example.exception.CustomException;
import com.example.mapper.QuestionMapper;
import com.example.mapper.QuestionOptionMapper;
import com.example.entity.QuestionOption;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.Date;
import java.util.List;

@Service
public class QuestionService {
    @Resource
    private QuestionMapper questionMapper;
    @Resource
    private QuestionOptionMapper questionOptionMapper;

    public void add(Question question) {
        questionMapper.insert(question);
    }

    public void addWithOptions(QuestionAddDTO questionAddDTO) {
        // 1. 添加题目主体
        Question question = new Question();
        question.setName(questionAddDTO.getName());
        question.setCourseId(questionAddDTO.getCourseId());
        question.setTypeId(questionAddDTO.getTypeId());
        question.setScore(questionAddDTO.getScore());
        question.setReferenceAnswer(questionAddDTO.getReferenceAnswer());
        
        questionMapper.insert(question);
        
        // 2. 如果是选择题，添加选项
        if (questionAddDTO.getOptions() != null && !questionAddDTO.getOptions().isEmpty()) {
            List<QuestionOption> questionOptions = questionAddDTO.getOptions().stream()
                    .map(dto -> {
                        QuestionOption option = new QuestionOption();
                        option.setQuestionId(question.getId());
                        option.setOptionLabel(dto.getOptionLabel());
                        option.setOptionContent(dto.getOptionContent());
                        return option;
                    })
                    .toList();
            questionOptionMapper.insertBatch(questionOptions);
        }
    }
    public PageInfo<QuestionListVO> selectPage(Integer pageNum, Integer pageSize,String name) {
        PageHelper.startPage(pageNum, pageSize);
        List<QuestionListVO> list = questionMapper.selectPage(name);
        return PageInfo.of(list);
    }

    public List<Question> selectAll(Question question) {
        return questionMapper.selectAll(question);
    }

    public Question selectById(Integer id) {
        return questionMapper.selectById(id);
    }

    public void updateById(Question question) {
        questionMapper.updateById(question);
    }

    public void updateWithOptions(QuestionAddDTO questionAddDTO) {
        // 1. 更新题目主体
        Question question = new Question();
        question.setId(questionAddDTO.getId());
        question.setName(questionAddDTO.getName());
        question.setCourseId(questionAddDTO.getCourseId());
        question.setTypeId(questionAddDTO.getTypeId());
        question.setScore(questionAddDTO.getScore());
        question.setReferenceAnswer(questionAddDTO.getReferenceAnswer());
        
        questionMapper.updateById(question);
        
        // 2. 如果是选择题，更新选项
        if (questionAddDTO.getOptions() != null && !questionAddDTO.getOptions().isEmpty()) {
            // 先删除原有选项
            questionOptionMapper.deleteByQuestionId(questionAddDTO.getId());
            
            // 再添加新选项
            List<QuestionOption> questionOptions = questionAddDTO.getOptions().stream()
                    .map(dto -> {
                        QuestionOption option = new QuestionOption();
                        option.setQuestionId(questionAddDTO.getId());
                        option.setOptionLabel(dto.getOptionLabel());
                        option.setOptionContent(dto.getOptionContent());
                        return option;
                    })
                    .toList();
            if (!questionOptions.isEmpty()) {
                questionOptionMapper.insertBatch(questionOptions);
            }
        }
    }

    public void deleteById(Integer id) {
        // 先删除选项
        questionOptionMapper.deleteByQuestionId(id);
        // 再删除题目
        questionMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            // 先删除选项
            questionOptionMapper.deleteByQuestionId(id);
            // 再删除题目
            questionMapper.deleteById(id);
        }
    }

}
