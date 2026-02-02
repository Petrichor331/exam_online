package com.example.service;


import cn.hutool.core.date.DateUtil;
import com.example.common.Constants;
import com.example.common.dto.QuestionAddDTO;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.common.vo.QuestionListVO;
import com.example.entity.Account;
import com.example.entity.Question;
import com.example.exception.CustomException;
import com.example.mapper.QuestionMapper;
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

    public void add(Question question) {
        questionMapper.insert(question);
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

    public void deleteById(Integer id) {
        questionMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            questionMapper.deleteById(id);
        }
    }

}
