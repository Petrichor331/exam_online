package com.example.service;


import cn.hutool.core.date.DateUtil;
import com.example.entity.QuestionType;
import com.example.exception.CustomException;
import com.example.mapper.QuestionTypeMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionTypeService {
    @Resource
    private QuestionTypeMapper questionTypeMapper;

    public void add(QuestionType questionType) {
        List<QuestionType> list = questionTypeMapper.selectByName(questionType.getName());
        if(list.size()>0){
            throw new CustomException("-1","题型名称不能重复");
        }
        questionTypeMapper.insert(questionType);
    }
    public PageInfo<QuestionType> selectPage(QuestionType questionType, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<QuestionType> list = questionTypeMapper.selectAll(questionType);
        return PageInfo.of(list);
    }

    public List<QuestionType> selectAll(QuestionType questionType) {
        return questionTypeMapper.selectAll(questionType);
    }

    public QuestionType selectById(Integer id) {
        return questionTypeMapper.selectById(id);
    }

    public void updateById(QuestionType questionType) {
        List<QuestionType> list = questionTypeMapper.selectByName(questionType.getName());
        if(list.size()>0 && !questionType.getId().equals(list.get(0).getId())){
            throw new CustomException("-1","题型名称不能重复");
        }
        questionTypeMapper.updateById(questionType);
    }

    public void deleteById(Integer id) {
        questionTypeMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            questionTypeMapper.deleteById(id);
        }
    }

}
