package com.example.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 邮箱验证码服务
 */
@Slf4j
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final String EMAIL_CAPTCHA_KEY = "email_captcha:";
    private static final int CAPTCHA_LENGTH = 6; // 6位验证码
    private static final String CAPTCHA_CHARS = "0123456789";
    private static final long EXPIRE_TIME = 5; // 5分钟过期

    /**
     * 生成随机验证码
     */
    private String generateCaptcha() {
        StringBuilder captcha = new StringBuilder();
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            int index = (int) (Math.random() * CAPTCHA_CHARS.length());
            captcha.append(CAPTCHA_CHARS.charAt(index));
        }
        return captcha.toString();
    }

    /**
     * 发送邮箱验证码
     * @param email 目标邮箱
     * @return 是否发送成功
     */
    public boolean sendEmailCaptcha(String email) {
        String captchaCode = generateCaptcha();
        String key = EMAIL_CAPTCHA_KEY + email;

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("注册验证码");
            message.setText("您的注册验证码是：" + captchaCode + "，有效期为5分钟，请尽快完成注册。");

            mailSender.send(message);

            // 存入Redis，5分钟过期
            redisTemplate.opsForValue().set(key, captchaCode, EXPIRE_TIME, TimeUnit.MINUTES);
            log.info("邮箱验证码已发送至 {}，验证码：{}", email, captchaCode);
            return true;
        } catch (Exception e) {
            log.error("发送邮箱验证码失败", e);
            return false;
        }
    }

    /**
     * 验证邮箱验证码
     * @param email 邮箱
     * @param captchaCode 用户输入的验证码
     * @return 是否验证成功
     */
    public boolean verifyEmailCaptcha(String email, String captchaCode) {
        String key = EMAIL_CAPTCHA_KEY + email;
        String storedCode = (String) redisTemplate.opsForValue().get(key);
        if (storedCode == null) {
            return false; // 验证码不存在或已过期
        }
        return storedCode.equals(captchaCode);
    }
}