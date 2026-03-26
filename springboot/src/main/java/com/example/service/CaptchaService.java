package com.example.service;

import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.codec.Base64;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class CaptchaService {
    
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String CAPTCHA_KEY = "captcha:";
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LENGTH = 4;
    private static final int LINE_COUNT = 3;
    
    /**
     * 生成验证码图片
     * 返回Map：captchaId, captchaImage
     */
    public Map<String, String> generateCaptchaMap() {
        LineCaptcha captcha = new LineCaptcha(WIDTH, HEIGHT, CODE_LENGTH, LINE_COUNT);
        String captchaCode = captcha.getCode();
        String captchaId = generateCaptchaId();
        
        // 存入Redis，5分钟过期
        redisTemplate.opsForValue().set(CAPTCHA_KEY + captchaId, captchaCode, 5, TimeUnit.MINUTES);
        
        // 生成base64图片
        String base64 = "data:image/png;base64," + Base64.encode(imageToBytes(captcha.getImage()));
        
        Map<String, String> result = new HashMap<>();
        result.put("captchaId", captchaId);
        result.put("captchaImage", base64);
        
        return result;
    }
    
    private byte[] imageToBytes(BufferedImage image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            javax.imageio.ImageIO.write(image, "png", baos);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
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
    
    private String generateCaptchaId() {
        return System.currentTimeMillis() + "" + new Random().nextInt(1000);
    }
}
