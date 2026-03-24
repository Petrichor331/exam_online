package com.example.controller;

import com.example.common.Result;
import com.example.service.AiService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
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

    /**
     * AI流式对话接口（SSE）
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, Object> params) {
        SseEmitter emitter = new SseEmitter(60000L); // 60秒超时
        
        String message = (String) params.get("message");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = (List<Map<String, String>>) params.get("history");

        if (message == null || message.trim().isEmpty()) {
            try {
                emitter.send(SseEmitter.event().name("error").data("消息不能为空"));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        // 异步处理流式响应
        new Thread(() -> {
            try {
                aiService.chatStream(message.trim(), history, content -> {
                    try {
                        emitter.send(SseEmitter.event().name("message").data(content));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                });
                emitter.complete();
            } catch (Exception e) {
                log.error("AI流式对话失败", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("AI服务暂时不可用"));
                } catch (IOException ioException) {
                    // ignore
                }
                emitter.complete();
            }
        }).start();

        return emitter;
    }
}
