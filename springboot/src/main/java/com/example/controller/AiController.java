package com.example.controller;

import com.example.common.Result;
import com.example.service.AiService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI学习助手控制器
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private AiService aiService;

    /**
     * AI对话接口
     */
    @PostMapping("/chat")
    public Result chat(@RequestBody Map<String, Object> params) {
        try {
            String message = (String) params.get("message");
            @SuppressWarnings("unchecked")
            List<Map<String, String>> history = (List<Map<String, String>>) params.get("history");

            if (message == null || message.trim().isEmpty()) {
                return Result.error("消息不能为空");
            }

            String reply = aiService.chat(message.trim(), history);
            return Result.success(reply);
        } catch (Exception e) {
            log.error("AI对话失败", e);
            return Result.error("AI服务暂时不可用");
        }
    }
}
