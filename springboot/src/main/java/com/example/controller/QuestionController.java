package com.example.controller;


import com.example.common.Result;
import com.example.common.dto.QuestionAddDTO;
import com.example.common.vo.ExamQuestionVO;
import com.example.common.vo.QuestionListVO;
import com.example.entity.Question;
import com.example.entity.QuestionOption;
import com.example.service.QuestionService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/question")
public class QuestionController {
    @Resource
    private QuestionService questionService;

    @PostMapping("/add")
    public Result add(@RequestBody QuestionAddDTO questionAddDTO){
        questionService.addWithOptions(questionAddDTO);
        return Result.success();
    }
    @GetMapping("/getKnowledgePoints")
    public Result getKnowledgePoints(@RequestParam Integer courseId){
        List<String> list = questionService.selectKnowledgePoints(courseId);
        return Result.success(list);
    }
    @GetMapping("/selectPage")
    public Result selectPage(
            @RequestParam Integer pageNum,
            @RequestParam Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) Integer typeId,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) List<String> knowledgePoints
    ) {
        PageInfo<QuestionListVO> questionListVO = questionService.selectPage(pageNum, pageSize, name, courseId, typeId, difficulty, knowledgePoints);
        return Result.success(questionListVO);
    }

    /**
     *
     * 通过题目id查题目具体信息
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id){
        Question question = questionService.selectById(id);
        return Result.success(question);
    }


    @GetMapping("/selectAll")
    //不管前端填了什么，都会返回所有数据
    public Result selectAll(Question question){
        List<Question> list = questionService.selectAll(question);
        return Result.success(list);
    }

    @PutMapping("/update")
    public Result update(@RequestBody QuestionAddDTO questionAddDTO){
        questionService.updateWithOptions(questionAddDTO);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id){
        questionService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids){
        questionService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 刷题 - 随机获取题目
     */
    @GetMapping("/random")
    public Result getRandomQuestions(
            @RequestParam Integer courseId,
            @RequestParam(required = false) List<Integer> typeIds,
            @RequestParam(required = false) List<String> knowledgePoints,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false, defaultValue = "10") Integer count
    ) {
        List<ExamQuestionVO> questions = questionService.getRandomQuestions(courseId, typeIds, knowledgePoints, difficulty, count);
        return Result.success(questions);
    }

    /**
     * 刷题 - 获取课程所有题目
     */
    @GetMapping("/list")
    public Result getQuestionList(
            @RequestParam Integer courseId,
            @RequestParam(required = false) List<Integer> typeIds,
            @RequestParam(required = false) List<String> knowledgePoints,
            @RequestParam(required = false) Integer difficulty
    ) {
        List<ExamQuestionVO> questions = questionService.getQuestionsByCourse(courseId, typeIds, knowledgePoints, difficulty);
        return Result.success(questions);
    }


}
