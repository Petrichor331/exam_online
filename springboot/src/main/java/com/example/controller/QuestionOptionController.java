package com.example.controller;

import com.example.common.Result;
import com.example.entity.QuestionOption;
import com.example.service.QuestionOptionService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/questionOption")
public class QuestionOptionController {
    @Resource
    private QuestionOptionService questionOptionService;

    @GetMapping("/selectByQuestionId/{questionId}")
    public Result selectByQuestionId(@PathVariable Integer questionId) {
        List<QuestionOption> options = questionOptionService.selectByQuestionId(questionId);
        return Result.success(options);
    }
}