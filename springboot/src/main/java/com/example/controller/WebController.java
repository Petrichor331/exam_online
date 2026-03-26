package com.example.controller;

import com.example.common.Result;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.service.AdminService;
import com.example.service.CaptchaService;
import com.example.service.EmailService;
import com.example.service.StudentService;
import com.example.service.TeacherService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    @Resource
    private EmailService emailService;

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

    /**
     * 发送邮箱验证码
     */
    @PostMapping("/sendEmailCaptcha")
    public Result sendEmailCaptcha(@RequestBody Map<String, String> params){
        String email = params.get("email");
        String captchaId = params.get("captchaId");
        String captchaCode = params.get("captchaCode");
        
        if(email == null || email.isEmpty()){
            return Result.error("邮箱不能为空");
        }
        
        // 验证图形验证码
        if(captchaId == null || captchaCode == null || !captchaService.verifyCaptcha(captchaId, captchaCode)){
            return Result.error("图形验证码错误");
        }
        
        boolean success = emailService.sendEmailCaptcha(email);
        if(success){
            return Result.success("验证码已发送，请查收");
        }else{
            return Result.error("验证码发送失败，请稍后重试");
        }
    }

    /**
     * 第一步：邮箱验证
     * 只验证邮箱和验证码，检查邮箱是否已注册，不创建账户
     */
    @PostMapping("/registerStep1")
    public Result registerStep1(@RequestBody Map<String, Object> params){
        String email = (String) params.get("email");
        String emailCaptcha = (String) params.get("emailCaptcha");
        String role = (String) params.get("role");
        
        // 基础参数验证
        if(email == null || email.isEmpty()){
            return Result.error("邮箱不能为空");
        }
        if(emailCaptcha == null || emailCaptcha.isEmpty()){
            return Result.error("邮箱验证码不能为空");
        }
        if(role == null || role.isEmpty()){
            return Result.error("角色不能为空");
        }
        
        // 验证邮箱验证码
        if(!emailService.verifyEmailCaptcha(email, emailCaptcha)){
            return Result.error("邮箱验证码错误或已过期");
        }
        
        // 检查邮箱是否已被注册
        Account existingAccount = checkEmailExists(email, role);
        if(existingAccount != null){
            return Result.error("该邮箱已被注册");
        }
        
        // 邮箱验证通过，返回成功（不创建账户）
        return Result.success("邮箱验证通过");
    }

    /**
     * 第二步：设置用户名和密码，创建账户
     */
    @PostMapping("/registerStep2")
    public Result registerStep2(@RequestBody Map<String, Object> params){
        try {
            String email = (String) params.get("email");
            String username = (String) params.get("username");
            String password = (String) params.get("password");
            String role = (String) params.get("role");
            
            if(email == null || email.isEmpty()){
                return Result.error("邮箱不能为空");
            }
            if(username == null || username.isEmpty()){
                return Result.error("用户名不能为空");
            }
            if(password == null || password.isEmpty()){
                return Result.error("密码不能为空");
            }
            if(role == null || role.isEmpty()){
                return Result.error("角色不能为空");
            }
            
            // 检查用户名是否已被占用
            if(checkUsernameExists(username, role, null)){
                return Result.error("用户名已被占用");
            }
            
            // 创建账户
            Account account = new Account();
            account.setEmail(email);
            account.setUsername(username);
            account.setPassword(password);
            account.setRole(role);
            
            Integer accountId = null;
            if(RoleEnum.TEACHER.name().equals(role)){
                accountId = teacherService.register(account);
            }else if(RoleEnum.STUDENT.name().equals(role)){
                accountId = studentService.register(account);
            }else if(RoleEnum.ADMIN.name().equals(role)){
                return Result.error("管理员账号禁止自助注册");
            }else{
                return Result.error("角色不存在");
            }
            
            // 返回账户ID，供后续步骤使用
            return Result.success(accountId);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("注册失败: " + e.getMessage());
        }
    }

    /**
     * 第三步：完善用户信息
     */
    @PostMapping("/registerStep3")
    public Result registerStep3(@RequestBody Map<String, Object> params){
        Integer accountId = (Integer) params.get("accountId");
        String role = (String) params.get("role");
        String name = (String) params.get("name");
        String phone = (String) params.get("phone");
        String avatar = (String) params.get("avatar");
        
        if(accountId == null){
            return Result.error("账户ID不能为空");
        }
        if(role == null || role.isEmpty()){
            return Result.error("角色不能为空");
        }
        
        // 更新用户信息
        boolean success = updateUserInfo(accountId, name, phone, avatar, role);
        if(success){
            return Result.success("用户信息完善成功");
        }else{
            return Result.error("设置失败，请重试");
        }
    }

    /**
     * 检查邮箱是否已注册
     */
    private Account checkEmailExists(String email, String role){
        if(RoleEnum.TEACHER.name().equals(role)){
            return teacherService.selectByEmail(email);
        }else if(RoleEnum.STUDENT.name().equals(role)){
            return studentService.selectByEmail(email);
        }
        return null;
    }

    /**
     * 检查用户名是否已存在
     */
    private boolean checkUsernameExists(String username, String role, Integer accountId){
        Account existing = null;
        if(RoleEnum.TEACHER.name().equals(role)){
            existing = teacherService.selectByUsername(username);
        }else if(RoleEnum.STUDENT.name().equals(role)){
            existing = studentService.selectByUsername(username);
        }
        // accountId为null时，只要存在就返回true
        if(accountId == null){
            return existing != null;
        }
        return existing != null && !existing.getId().equals(accountId);
    }

    /**
     * 更新用户信息
     */
    private boolean updateUserInfo(Integer accountId, String name, String phone, String avatar, String role){
        try{
            if(RoleEnum.TEACHER.name().equals(role)){
                teacherService.updateUserInfo(accountId, name, phone, avatar);
            }else if(RoleEnum.STUDENT.name().equals(role)){
                studentService.updateUserInfo(accountId, name, phone, avatar);
            }
            return true;
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }
}
