package com.example.controller;


import com.example.common.Result;
import com.example.entity.ExamPlan;
import com.example.service.ExamPlanService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/examPlan")
public class ExamPlanController {

    @Resource
    private ExamPlanService examPlanService;
    @PostMapping("/add")
    public Result add(@RequestBody ExamPlan examPlan){
        examPlanService.add(examPlan);
        return Result.success();
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(ExamPlan examPlan,
                            @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize){
        PageInfo<ExamPlan> pageInfo =examPlanService.selectPage(examPlan,pageNum,pageSize);
        return Result.success(pageInfo);
    }


    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id){
        ExamPlan examPlan = examPlanService.selectById(id);
        return Result.success(examPlan);
    }

    @GetMapping("/selectAll")
    //不管前端填了什么，都会返回所有数据
    public Result selectAll(ExamPlan examPlan){
        List<ExamPlan> list = examPlanService.selectAll(examPlan);
        return Result.success(list);
    }

    @PutMapping("/update")
    public Result update(@RequestBody ExamPlan examPlan){
        examPlanService.updateById(examPlan);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id){
        examPlanService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids){
        examPlanService.deleteBatch(ids);
        return Result.success();
    }
}
