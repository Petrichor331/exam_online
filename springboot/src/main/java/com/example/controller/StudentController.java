package com.example.controller;


import com.example.common.Result;
import com.example.entity.Student;
import com.example.service.StudentService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/student")
public class StudentController {

    @Resource
    private StudentService studentService;
    @PostMapping("/add")
    //对应后台管理那个页面里面的新增学生，不是注册
    public Result add(@RequestBody Student student){
        studentService.add(student);
        return Result.success();
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(Student student,
                            @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize){
        PageInfo<Student> pageInfo =studentService.selectPage(student,pageNum,pageSize);
        return Result.success(pageInfo);
    }


    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id){
        Student student = studentService.selectById(id);
        return Result.success(student);
    }

    @GetMapping("/selectAll")
    //不管前端填了什么，都会返回所有数据
    public Result selectAll(Student student){
        List<Student> list = studentService.selectAll(student);
        return Result.success(list);
    }

    @PutMapping("/update")
    public Result update(@RequestBody Student student){
        studentService.updateById(student);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids){
        studentService.deleteBatch(ids);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id){
        studentService.deleteById(id);
        return Result.success();
    }


}
