package com.example.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class UserDisableService {
    
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String DISABLED_KEY = "user:disabled:";
    
    /**
     * 禁用用户
     * @param days 禁用天数，0表示永久禁用
     */
    public void disableUser(String role, Integer userId, int days) {
        String key = DISABLED_KEY + role + ":" + userId;
        if (days <= 0) {
            // 永久禁用
            redisTemplate.opsForValue().set(key, "forever");
        } else {
            // 临时禁用，设置过期时间
            LocalDateTime expiryTime = LocalDateTime.now().plusDays(days);
            redisTemplate.opsForValue().set(key, expiryTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
    }
    
    /**
     * 解除禁用
     */
    public void enableUser(String role, Integer userId) {
        String key = DISABLED_KEY + role + ":" + userId;
        redisTemplate.delete(key);
    }
    
    /**
     * 检查用户是否被禁用，返回错误信息
     */
    public String checkUserStatus(String role, Integer userId) {
        String key = DISABLED_KEY + role + ":" + userId;
        Object value = redisTemplate.opsForValue().get(key);
        
        if (value == null) {
            return null;
        }
        
        String disableInfo = value.toString();
        if ("forever".equals(disableInfo)) {
            return "账号已被永久禁用，请联系管理员";
        }
        
        try {
            LocalDateTime expiryTime = LocalDateTime.parse(disableInfo, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(expiryTime)) {
                // 已过期，自动解除
                redisTemplate.delete(key);
                return null;
            }
            long days = Duration.between(now, expiryTime).toDays() + 1;
            return "账号已被禁用，剩余" + days + "天";
        } catch (Exception e) {
            return null;
        }
    }
}
