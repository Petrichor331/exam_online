package com.example.controller;


import com.example.common.Result;
import com.example.entity.Course;
import com.example.service.CourseService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/course")
public class CourseController {

    @Resource
    private CourseService courseService;
    @PostMapping("/add")
    public Result add(@RequestBody Course course){
        courseService.add(course);
        return Result.success();
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(Course course,
                            @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize){
        PageInfo<Course> pageInfo =courseService.selectPage(course,pageNum,pageSize);
        return Result.success(pageInfo);
    }


    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id){
        Course course = courseService.selectById(id);
        return Result.success(course);
    }

    @GetMapping("/selectAll")
    //不管前端填了什么，都会返回所有数据
    public Result selectAll(Course course){
        List<Course> list = courseService.selectAll(course);
        return Result.success(list);
    }

    @PutMapping("/update")
    public Result update(@RequestBody Course course){
        courseService.updateById(course);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id){
        courseService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids){
        courseService.deleteBatch(ids);
        return Result.success();
    }
}
