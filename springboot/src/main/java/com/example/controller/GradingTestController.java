package com.example.controller;

import com.example.common.Result;
import com.example.service.PythonGradingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AI评分测试接口
 * 用于测试Python AI评分服务
 */
@Slf4j
@RestController
@RequestMapping("/api/grading")
public class GradingTestController {
    
    @Autowired
    private PythonGradingService pythonGradingService;
    
    /**
     * 检查Python服务状态
     */
    @GetMapping("/health")
    public Result healthCheck() {
        boolean isHealthy = pythonGradingService.healthCheck();
        if (isHealthy) {
            return Result.success("Python AI评分服务正常运行");
        } else {
            return Result.error("500", "Python AI评分服务未启动或不可用");
        }
    }
    
    /**
     * 测试单题评分
     */
    @PostMapping("/test")
    public Result testGrading(@RequestBody Map<String, Object> params) {
        try {
            String standardAnswer = (String) params.get("standardAnswer");
            String studentAnswer = (String) params.get("studentAnswer");
            Integer maxScore = (Integer) params.getOrDefault("maxScore", 10);
            
            if (standardAnswer == null || studentAnswer == null) {
                return Result.error("400", "标准答案和学生答案不能为空");
            }
            
            PythonGradingService.AIGradingResult result = 
                pythonGradingService.gradeSingle(standardAnswer, studentAnswer, maxScore);
            
            if (result.isSuccess()) {
                Map<String, Object> data = new HashMap<>();
                data.put("score", result.getScore());
                data.put("maxScore", result.getMaxScore());
                data.put("similarity", result.getSimilarity());
                return Result.success(data);
            } else {
                return Result.error("500", "评分失败: " + result.getMessage());
            }
            
        } catch (Exception e) {
            log.error("测试评分失败", e);
            return Result.error("500", "测试评分失败: " + e.getMessage());
        }
    }
    
    /**
     * 示例评分测试
     * 快速测试，无需参数
     */
    @GetMapping("/demo")
    public Result demoGrading() {
        try {
            String standardAnswer = "Java面向对象的三大特性是封装、继承、多态。" +
                "封装是指将数据和操作数据的方法绑定在一起，隐藏内部实现细节；" +
                "继承是指子类可以继承父类的属性和方法，实现代码复用；" +
                "多态是指同一操作作用于不同的对象可以产生不同的行为，提高程序的可扩展性。";
            
            String studentAnswer = "三大特性：封装、继承、多态。" +
                "封装就是把数据和方法包在一起，隐藏内部细节；" +
                "继承是子类继承父类的特性，可以重用代码；" +
                "多态是不同的对象对同一个消息有不同的响应方式。";
            
            PythonGradingService.AIGradingResult result = 
                pythonGradingService.gradeSingle(standardAnswer, studentAnswer, 10);
            
            if (result.isSuccess()) {
                Map<String, Object> data = new HashMap<>();
                data.put("standardAnswer", standardAnswer);
                data.put("studentAnswer", studentAnswer);
                data.put("score", result.getScore());
                data.put("maxScore", result.getMaxScore());
                data.put("similarity", result.getSimilarity());
                return Result.success(data);
            } else {
                return Result.error("500", "评分失败: " + result.getMessage());
            }
            
        } catch (Exception e) {
            log.error("示例评分失败", e);
            return Result.error("500", "示例评分失败: " + e.getMessage());
        }
    }
}
