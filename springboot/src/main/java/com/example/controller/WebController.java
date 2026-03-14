package com.example.controller;

import com.example.common.Result;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.service.AdminService;
import com.example.service.CaptchaService;
import com.example.service.StudentService;
import com.example.service.TeacherService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

import javax.management.relation.Role;

@RestController
public class WebController {

    @Resource
    private AdminService adminService;

    @Resource
    private StudentService studentService;

    @Resource
    private TeacherService teacherService;

    @Resource
    private CaptchaService captchaService;

    @GetMapping("/")
    public Result hello(){
        return Result.success("这是springboot3的项目工程,后端启动成功");
    }

    @PostMapping("/login")
    public Result login(@RequestBody Map<String, Object> params){
        // 验证验证码
        String captchaId = (String) params.get("captchaId");
        String captchaCode = (String) params.get("captchaCode");
        
        if (captchaId == null || captchaCode == null || !captchaService.verifyCaptcha(captchaId, captchaCode)) {
            return Result.error("验证码错误");
        }
        
        // 从Map中取账号信息
        Account account = new Account();
        account.setUsername((String) params.get("username"));
        account.setPassword((String) params.get("password"));
        account.setRole((String) params.get("role"));
        
        Account loginAccount = null;
        /*管理员登录*/
        if(RoleEnum.valueOf(account.getRole()) == RoleEnum.ADMIN){
            loginAccount = adminService.login(account);
        }else if(RoleEnum.TEACHER.name().equals(account.getRole())){
            loginAccount = teacherService.login(account);
        }else if(RoleEnum.STUDENT.name().equals(account.getRole())){
            loginAccount = studentService.login(account);
        }
        return Result.success(loginAccount);
    }


    @PostMapping("/register")
    /* 通过account来接收前端传来的json参数 */
    public Result register(@RequestBody Account account){
        if(RoleEnum.TEACHER.name().equals(account.getRole())){
            teacherService.register(account);
        }
        if(RoleEnum.STUDENT.name().equals(account.getRole())){
            studentService.register(account);
        }
        return Result.success();
    }


    @PutMapping("/updatePassword")
    public Result updatePassword(@RequestBody Account account){
        if(RoleEnum.ADMIN.name().equals(account.getRole())){
            adminService.updatePassword(account);
        }else if(RoleEnum.TEACHER.name().equals(account.getRole())){
            teacherService.updatePassword(account);
        }else if(RoleEnum.STUDENT.name().equals(account.getRole())){
            studentService.updatePassword(account);
        }
        return Result.success();
    }
}
