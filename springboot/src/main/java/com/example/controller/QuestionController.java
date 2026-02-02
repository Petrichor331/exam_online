package com.example.controller;


import com.example.common.Result;
import com.example.common.dto.QuestionAddDTO;
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
    public Result add(@RequestBody Question question){
        questionService.add(question);
        return Result.success();
    }
    @GetMapping("/selectPage")
    public PageInfo<QuestionListVO> selectPage(
            @RequestParam Integer pageNum,
            @RequestParam Integer pageSize,
            @RequestParam(required = false) String name
    ) {
        PageInfo<QuestionListVO> questionListVO = questionService.selectPage(pageNum, pageSize, name);
        return questionListVO;
    }
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

//    @PutMapping("/update")
//    public Result update(@RequestBody QuestionAddDTO questionAddDTO){
//
//        questionService.updateById();
//        return Result.success();
//    }

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
    

}
