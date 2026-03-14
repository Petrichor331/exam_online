package com.example.controller;


import com.example.common.Result;
import com.example.entity.Admin;
import com.example.service.AdminService;
import com.example.service.UserDisableService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private AdminService adminService;
    @Resource
    private UserDisableService userDisableService;
    
    /**
     * 禁用用户
     *
     */
    @PostMapping("/disable")
    public Result disableUser(@RequestBody Map<String, Object> params) {
        String role = (String) params.get("role");
        Integer userId = (Integer) params.get("userId");
        Integer days = (Integer) params.get("days");
        
        if (role == null || userId == null) {
            return Result.error("参数错误");
        }
        
        userDisableService.disableUser(role, userId, days != null ? days : 0);
        return Result.success("禁用成功");
    }
    
    /**
     * 解除禁用
     */
    @PostMapping("/enable")
    public Result enableUser(@RequestBody Map<String, Object> params) {
        String role = (String) params.get("role");
        Integer userId = (Integer) params.get("userId");
        
        if (role == null || userId == null) {
            return Result.error("参数错误");
        }
        
        userEnable(role, userId);
        return Result.success("解除成功");
    }
    
    private void userEnable(String role, Integer userId) {
        userDisableService.enableUser(role, userId);
    }
    @PostMapping("/add")
    public Result add(@RequestBody Admin admin){
        adminService.add(admin);
        return Result.success();
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(Admin admin,
                            @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize){
        PageInfo<Admin> pageInfo =adminService.selectPage(admin,pageNum,pageSize);
        return Result.success(pageInfo);
    }


    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id){
        Admin admin = adminService.selectById(id);
        return Result.success(admin);
    }

    @GetMapping("/selectAll")
    //不管前端填了什么，都会返回所有数据
    public Result selectAll(Admin admin){
        List<Admin> list = adminService.selectAll(admin);
        return Result.success(list);
    }

    @PutMapping("/update")
    public Result update(@RequestBody Admin admin){
        adminService.updateById(admin);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result delete(@RequestBody List<Integer> ids){
        adminService.deleteBatch(ids);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id){
        adminService.deleteById(id);
        return Result.success();
    }


}
