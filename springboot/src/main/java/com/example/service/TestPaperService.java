package com.example.service;


import cn.hutool.core.date.DateUtil;
import com.example.common.Result;
import com.example.common.dto.TestPaperAddDTO;
import com.example.common.vo.TestPaperVO;
import com.example.entity.Account;
import com.example.entity.TestPaper;
import com.example.exception.CustomException;
import com.example.mapper.QuestionMapper;
import com.example.mapper.TestPaperMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.apache.el.parser.Token;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TestPaperService {
    @Resource
    private TestPaperMapper testPaperMapper;

    @Resource
    private QuestionMapper questionMapper;

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
        //根据出卷方式生成id
        String questionIds;
        if("manual".equals(testPaperAddDTO.getGenerateType())){
            //手动组卷：使用前端传来的题目ID列表
            List<Integer> questionIdList = testPaperAddDTO.getQuestionIds();
            if(ObjectUtils.isEmpty(questionIdList)){
                throw new CustomException("手动组卷需要至少选择一道题目");
            }
            questionIds = questionIdList.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
        }
        else if("auto".equals(testPaperAddDTO.getGenerateType())){
            //TODO
            questionIds = autoGenerateQuestions(testPaperAddDTO.getCourseId());
        }else{
            throw new CustomException("请选择出卷方式");
        }

        TestPaper testPaper = new TestPaper();
        testPaper.setCourseId(testPaperAddDTO.getCourseId());
        testPaper.setName(testPaperAddDTO.getPaperName());
        testPaper.setStart(testPaperAddDTO.getStartTime());
        testPaper.setEnd(testPaperAddDTO.getEndTime());
        testPaper.setTime(testPaperAddDTO.getDuration());
        testPaper.setQuestionIds(questionIds);
        testPaper.setType(testPaperAddDTO.getGenerateType());

        Account account = TokenUtils.getCurrentUser();
        System.out.println("当前用户: " + account);
        if (account != null) {
            System.out.println("用户角色: " + account.getRole());
            if ("TEACHER".equalsIgnoreCase(account.getRole())) {
                testPaper.setTeacherId(account.getId());
                System.out.println("设置教师ID: " + account.getId());
            }
        }
        testPaperMapper.insert(testPaper);
        return Result.success();

    }
    /**
     * 自动组卷（后续可替换为遗传算法）
     */
    private String autoGenerateQuestions(Integer courseId) {

        // 从题库取候选题
        List<Integer> candidateIds =
                questionMapper.selectIdsByCourseId(courseId);

        if (candidateIds.size() < 10) {
            throw new CustomException("题库题目数量不足，无法自动组卷");
        }

        // ② 先简单随机选 10 道（占位）
        Collections.shuffle(candidateIds);
        List<Integer> selected = candidateIds.subList(0, 10);

        // ③ 转成 "1,2,3,4"
        return selected.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    public PageInfo<TestPaperVO> selectPage(TestPaper testPaper, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<TestPaperVO> list = testPaperMapper.selectAll(testPaper);
        return PageInfo.of(list);
    }

    public List<TestPaperVO> selectAll(TestPaper testPaper) {
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


