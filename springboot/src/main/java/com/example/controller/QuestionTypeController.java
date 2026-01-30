package com.example.controller;


import com.example.common.Result;
import com.example.entity.QuestionType;
import com.example.service.QuestionTypeService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/questionType")
public class QuestionTypeController {

    @Resource
    private QuestionTypeService questionTypeService;
    @PostMapping("/add")
    public Result add(@RequestBody QuestionType questionType){
        questionTypeService.add(questionType);
        return Result.success();
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(QuestionType questionType,
                            @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize){
        PageInfo<QuestionType> pageInfo =questionTypeService.selectPage(questionType,pageNum,pageSize);
        return Result.success(pageInfo);
    }


    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id){
        QuestionType questionType = questionTypeService.selectById(id);
        return Result.success(questionType);
    }

    @GetMapping("/selectAll")
    //不管前端填了什么，都会返回所有数据
    public Result selectAll(QuestionType questionType){
        List<QuestionType> list = questionTypeService.selectAll(questionType);
        return Result.success(list);
    }

    @PutMapping("/update")
    public Result update(@RequestBody QuestionType questionType){
        questionTypeService.updateById(questionType);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id){
        questionTypeService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids){
        questionTypeService.deleteBatch(ids);
        return Result.success();
    }
}
