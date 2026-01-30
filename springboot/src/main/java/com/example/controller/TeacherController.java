package com.example.controller;


import com.example.common.Result;
import com.example.entity.Teacher;
import com.example.service.TeacherService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/teacher")
public class TeacherController {

    @Resource
    private TeacherService teacherService;
    @PostMapping("/add")
    //对应后台管理那个页面里面的新增教师
    public Result add(@RequestBody Teacher teacher){
        teacherService.add(teacher);
        return Result.success();
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(Teacher teacher,
                            @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize){
        PageInfo<Teacher> pageInfo =teacherService.selectPage(teacher,pageNum,pageSize);
        return Result.success(pageInfo);
    }


    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id){
        Teacher teacher = teacherService.selectById(id);
        return Result.success(teacher);
    }

    @GetMapping("/selectAll")
    //不管前端填了什么，都会返回所有数据
    public Result selectAll(Teacher teacher){
        List<Teacher> list = teacherService.selectAll(teacher);
        return Result.success(list);
    }

    @PutMapping("/update")
    public Result update(@RequestBody Teacher teacher){
        teacherService.updateById(teacher);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids){
        teacherService.deleteBatch(ids);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id){
        teacherService.deleteById(id);
        return Result.success();
    }


}
