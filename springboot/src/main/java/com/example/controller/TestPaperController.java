package com.example.controller;


import com.example.common.Result;
import com.example.common.dto.TestPaperAddDTO;
import com.example.entity.TestPaper;
import com.example.service.TestPaperService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/testPaper")
public class TestPaperController {

    @Resource
    private TestPaperService testPaperService;
    @PostMapping("/add")
    public Result add(@RequestBody TestPaperAddDTO testPaperAddDTO){
        testPaperService.add(testPaperAddDTO);
        return Result.success();
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(TestPaper testPaper,
                            @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize){
        PageInfo<TestPaper> pageInfo =testPaperService.selectPage(testPaper,pageNum,pageSize);
        return Result.success(pageInfo);
    }


    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id){
        TestPaper testPaper = testPaperService.selectById(id);
        return Result.success(testPaper);
    }

    @GetMapping("/selectAll")
    //不管前端填了什么，都会返回所有数据
    public Result selectAll(TestPaper testPaper){
        List<TestPaper> list = testPaperService.selectAll(testPaper);
        return Result.success(list);
    }

    @PutMapping("/update")
    public Result update(@RequestBody TestPaper testPaper){
        testPaperService.updateById(testPaper);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id){
        testPaperService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids){
        testPaperService.deleteBatch(ids);
        return Result.success();
    }
}
