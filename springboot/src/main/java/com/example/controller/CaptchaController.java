package com.example.controller;

import com.example.common.Result;
import com.example.service.CaptchaService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/captcha")
public class CaptchaController {
    
    @Resource
    private CaptchaService captchaService;
    
    /**
     * 获取验证码图片
     */
    @GetMapping
    public Result getCaptcha() {
        Map<String, String> result = captchaService.generateCaptchaMap();
        return Result.success(result);
    }
    
    /**
     * 验证验证码
     */
    @PostMapping("/verify")
    public Result verify(@RequestParam String captchaId, @RequestParam String captchaCode) {
        boolean success = captchaService.verifyCaptcha(captchaId, captchaCode);
        return Result.success(success);
    }
}
