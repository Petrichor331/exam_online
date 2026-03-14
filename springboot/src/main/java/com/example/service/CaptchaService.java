package com.example.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class CaptchaService {
    
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String CAPTCHA_KEY = "captcha:";
    private static final int CAPTCHA_LENGTH = 4;
    private static final String CAPTCHA_CHARS = "0123456789";
    
    /**
     * 生成纯数字验证码
     * 返回格式：captchaId,captchaCode（测试用）
     */
    public String generateCaptcha() {
        String captchaCode = generateCode();
        String captchaId = generateCaptchaId();
        
        // 存入Redis，5分钟过期
        redisTemplate.opsForValue().set(CAPTCHA_KEY + captchaId, captchaCode, 5, TimeUnit.MINUTES);
        
        return captchaId + "," + captchaCode;
    }
    
    /**
     * 验证验证码
     */
    public boolean verifyCaptcha(String captchaId, String captchaCode) {
        if (captchaId == null || captchaCode == null) {
            return false;
        }
        
        String key = CAPTCHA_KEY + captchaId;
        Object cachedCode = redisTemplate.opsForValue().get(key);
        
        if (cachedCode == null) {
            return false;
        }
        
        // 验证成功后删除验证码
        redisTemplate.delete(key);
        
        return cachedCode.toString().equalsIgnoreCase(captchaCode);
    }
    
    private String generateCode() {
        StringBuilder code = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            code.append(CAPTCHA_CHARS.charAt(random.nextInt(CAPTCHA_CHARS.length())));
        }
        return code.toString();
    }
    
    private String generateCaptchaId() {
        return System.currentTimeMillis() + "" + new Random().nextInt(1000);
    }
}
