package com.example.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final RestTemplate restTemplate = new RestTemplate();

    // 系统提示词，定义AI助手的角色
    private static final String SYSTEM_PROMPT = "你是一个在线考试系统的学习助手，主要帮助学生解答学习问题、分析题目、提供解题思路。" +
            "请用简洁明了的语言回答，适当使用Markdown格式。" +
            "如果学生询问与学习无关的问题，请礼貌地引导他们回到学习话题。";

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
}
