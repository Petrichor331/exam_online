package com.example.controller;

import com.example.common.Result;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.service.AdminService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
public class WebController {

    @Resource
    private AdminService adminService;

    @GetMapping("/")
    public Result hello(){
        return Result.success("这是springboot3的项目工程,后端启动成功");
    }

    @PostMapping("/login")
    /* 通过account来接收前端传来的json参数 */
    public Result login(@RequestBody Account account){
        Account loginAccount = null;
        /*管理员登录*/
        if(RoleEnum.valueOf(account.getRole()) == RoleEnum.ADMIN){
            loginAccount = adminService.login(account);
        }
        return Result.success(loginAccount);
    }


    @PostMapping("/register")
    /* 通过account来接收前端传来的json参数 */
    public Result register(@RequestBody Account account){
        return Result.success();
    }


    @PutMapping("/updatePassword")
    public Result updatePassword(@RequestBody Account account){
        if(RoleEnum.ADMIN.name().equals(account.getRole())){
            adminService.updatePassword(account);
        }
        return Result.success();
    }
}
