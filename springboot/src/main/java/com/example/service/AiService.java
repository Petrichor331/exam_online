package com.example.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * AI学习助手服务
 */
@Slf4j
@Service
public class AiService {

    @Value("${ai.siliconflow.api-key:}")
    private String apiKey;

    @Value("${ai.siliconflow.api-url:https://api.siliconflow.cn/v1/chat/completions}")
    private String apiUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 系统提示词，定义AI助手的角色
    private static final String SYSTEM_PROMPT = "你是一个在线考试系统的学习助手，主要帮助学生解答学习问题、分析题目、提供解题思路。" +
            "请用简洁明了的语言回答，适当使用Markdown格式。" +
            "如果学生询问与学习无关的问题，请礼貌地引导他们回到学习话题。";

    // 缓存键前缀
    private static final String CACHE_KEY_PREFIX = "ai:chat:";

    // 缓存过期时间（小时）
    private static final int CACHE_EXPIRE_HOURS = 24;

    /**
     * 生成缓存键
     */
    private String generateCacheKey(String message, List<Map<String, String>> history) {
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append(message);
        if (history != null && !history.isEmpty()) {
            for (Map<String, String> item : history) {
                // 包含role和content以区分不同角色的相同内容
                contentBuilder.append(item.getOrDefault("role", ""));
                contentBuilder.append(item.getOrDefault("content", ""));
            }
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(contentBuilder.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return CACHE_KEY_PREFIX + hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // 这种情况理论上不会发生，但为了安全，回退到原来的方法
            log.error("SHA-256 algorithm not available, falling back to default hash", e);
            // 回退到简单的哈希（不推荐用于生产，但作为后备）
            int result = Objects.hash(message, history);
            return CACHE_KEY_PREFIX + result;
        }
    }

    /**
     * 发送消息给AI并获取回复
     * @param message 用户消息
     * @param history 历史对话记录
     * @return AI回复内容
     */
    public String chat(String message, List<Map<String, String>> history) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "AI服务未配置，请联系管理员配置API Key";
        }

        // 检查缓存
        String cacheKey = generateCacheKey(message, history);
        Object cachedResponse = redisTemplate.opsForValue().get(cacheKey);
        if (cachedResponse != null) {
            log.info("从缓存获取AI回复，缓存键: {}", cacheKey);
            return (String) cachedResponse;
        }

        try {
            // 构建消息列表
            JSONArray messages = new JSONArray();

            // 添加系统提示
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", SYSTEM_PROMPT);
            messages.add(systemMsg);

            // 添加历史对话（最多保留最近10条）
            if (history != null && !history.isEmpty()) {
                int start = Math.max(0, history.size() - 10);
                for (int i = start; i < history.size(); i++) {
                    Map<String, String> item = history.get(i);
                    JSONObject msg = new JSONObject();
                    msg.put("role", item.get("role"));
                    msg.put("content", item.get("content"));
                    messages.add(msg);
                }
            }

            // 添加当前用户消息
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", message);
            messages.add(userMsg);

            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "deepseek-ai/DeepSeek-V3");
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 2048);
            requestBody.put("temperature", 0.7);

            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> request = new HttpEntity<>(requestBody.toJSONString(), headers);

            // 发送请求
            log.info("调用DeepSeek API，消息长度: {}", message.length());
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);

            // 解析响应
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject result = JSON.parseObject(response.getBody());
                JSONArray choices = result.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JSONObject firstChoice = choices.getJSONObject(0);
                    JSONObject msgObj = firstChoice.getJSONObject("message");
                    String content = msgObj.getString("content");
                    log.info("AI回复成功，长度: {}", content.length());
                    
                    // 存入缓存
                    redisTemplate.opsForValue().set(cacheKey, content, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
                    log.info("AI回复已缓存，缓存键: {}", cacheKey);
                    
                    return content;
                }
            }

            log.error("AI回复格式异常: {}", response.getBody());
            return "AI服务暂时不可用，请稍后再试";

        } catch (Exception e) {
            log.error("调用AI服务失败", e);
            return "AI服务调用失败: " + e.getMessage();
        }
    }

    /**
     * 简单对话（无历史记录）
     */
    public String chat(String message) {
        return chat(message, new ArrayList<>());
    }

    /**
     * 流式对话接口
     */
    public interface StreamCallback {
        void onMessage(String content);
    }

    /**
     * 流式对话方法
     * @param message 用户消息
     * @param history 历史对话记录
     * @param callback 流式回调
     */
    public void chatStream(String message, List<Map<String, String>> history, StreamCallback callback) {
        if (apiKey == null || apiKey.isEmpty()) {
            callback.onMessage("AI服务未配置，请联系管理员配置API Key");
            return;
        }

        try {
            // 构建消息列表
            JSONArray messages = new JSONArray();

            // 添加系统提示
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", SYSTEM_PROMPT);
            messages.add(systemMsg);

            // 添加历史对话（最多保留最近10条）
            if (history != null && !history.isEmpty()) {
                int start = Math.max(0, history.size() - 10);
                for (int i = start; i < history.size(); i++) {
                    Map<String, String> item = history.get(i);
                    JSONObject msg = new JSONObject();
                    msg.put("role", item.get("role"));
                    msg.put("content", item.get("content"));
                    messages.add(msg);
                }
            }

            // 添加当前用户消息
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", message);
            messages.add(userMsg);

            // 构建请求体，启用流式响应
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "deepseek-ai/DeepSeek-V3");
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 2048);
            requestBody.put("temperature", 0.7);
            requestBody.put("stream", true);  // 启用流式响应

            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> request = new HttpEntity<>(requestBody.toJSONString(), headers);

            // 发送流式请求
            log.info("调用DeepSeek流式API，消息长度: {}", message.length());
            
            // 使用StreamingRestTemplate或手动处理流式响应
            // 这里我们使用一个简单的方法：手动处理流式响应
            processStreamResponse(request, callback);
            
        } catch (Exception e) {
            log.error("调用AI流式服务失败", e);
            callback.onMessage("AI服务调用失败: " + e.getMessage());
        }
    }

    /**
     * 处理流式响应
     */
    private void processStreamResponse(HttpEntity<String> request, StreamCallback callback) {
        // 注意：这里需要实现真正的流式处理
        // 由于Spring的RestTemplate不支持流式响应，我们需要使用WebClient或HttpClient
        // 作为临时方案，我们可以先使用非流式响应，然后逐步发送
        // 这里我们使用一个模拟的流式响应作为演示
        String response = restTemplate.postForObject(apiUrl, request, String.class);
        if (response != null) {
            // 模拟流式响应：将响应分成小块发送
            simulateStream(response, callback);
        } else {
            callback.onMessage("AI服务暂时不可用，请稍后再试");
        }
    }

    /**
     * 模拟流式响应（临时方案）
     */
    private void simulateStream(String content, StreamCallback callback) {
        // 将内容按句子分割，模拟流式效果
        String[] sentences = content.split("(?<=[.!?。！？])\\s*");
        for (String sentence : sentences) {
            if (!sentence.trim().isEmpty()) {
                callback.onMessage(sentence);
                try {
                    Thread.sleep(50); // 模拟延迟
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}
