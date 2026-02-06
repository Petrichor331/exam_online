package com.example.service;


import cn.hutool.core.date.DateUtil;
import com.example.common.Result;
import com.example.common.dto.TestPaperAddDTO;
import com.example.entity.TestPaper;
import com.example.exception.CustomException;
import com.example.mapper.TestPaperMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;

@Service
public class TestPaperService {
    @Resource
    private TestPaperMapper testPaperMapper;

    public Result add(TestPaperAddDTO testPaperAddDTO) {
        //首先参数校验
        if(ObjectUtils.isEmpty(testPaperAddDTO.getCourseId())){
            return Result.error("请选择课程");
        }
        if(ObjectUtils.isEmpty(testPaperAddDTO.getPaperName())){
            return Result.error("请输入试卷名称");
        }
        if(ObjectUtils.isEmpty(testPaperAddDTO.getStartTime()) || ObjectUtils.isEmpty(testPaperAddDTO.getEndTime())){
            return Result.error("请选择考试时间");
        }

        TestPaper testPaper = new TestPaper();
        testPaperMapper.insert(testPaper);
        return Result.success();
    }
    public PageInfo<TestPaper> selectPage(TestPaper testPaper, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<TestPaper> list = testPaperMapper.selectAll(testPaper);
        return PageInfo.of(list);
    }

    public List<TestPaper> selectAll(TestPaper testPaper) {
        return testPaperMapper.selectAll(testPaper);
    }

    public TestPaper selectById(Integer id) {
        return testPaperMapper.selectById(id);
    }

    public void updateById(TestPaper testPaper) {
        testPaperMapper.updateById(testPaper);
    }

    public void deleteById(Integer id) {
        testPaperMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            testPaperMapper.deleteById(id);
        }
    }

}
