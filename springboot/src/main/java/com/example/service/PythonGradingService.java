package com.example.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.entity.Question;
import com.example.entity.StudentAnswer;
import com.example.mapper.QuestionMapper;
import com.example.mapper.StudentAnswerMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Python AI评分服务
 * 调用Python提供的HTTP接口进行主观题评分
 */
@Slf4j
@Service
public class PythonGradingService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private QuestionMapper questionMapper;
    
    @Autowired
    private StudentAnswerMapper studentAnswerMapper;
    
    @Value("${python.service.url:http://localhost:5000}")
    private String pythonServiceUrl;
    
    /**
     * 检查Python服务是否健康
     */
    public boolean healthCheck() {
        try {
            String url = pythonServiceUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.error("Python服务健康检查失败", e);
            return false;
        }
    }
    
    /**
     * 单题评分
     * @param standardAnswer 标准答案
     * @param studentAnswer 学生答案
     * @param maxScore 满分
     * @return AI评分结果
     */
    public AIGradingResult gradeSingle(String standardAnswer, String studentAnswer, Integer maxScore) {
        try {
            String url = pythonServiceUrl + "/grade";
            
            // 构建请求参数
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("standardAnswer", standardAnswer);
            requestBody.put("studentAnswer", studentAnswer);
            requestBody.put("maxScore", maxScore);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject jsonResponse = JSON.parseObject(response.getBody());
                
                if (jsonResponse.getBoolean("success")) {
                    JSONObject data = jsonResponse.getJSONObject("data");
                    
                    AIGradingResult result = new AIGradingResult();
                    result.setScore(data.getInteger("score"));
                    result.setMaxScore(data.getInteger("maxScore"));
                    result.setSimilarity(data.getDouble("similarity"));
                    result.setSuccess(true);
                    
                    return result;
                }
            }
            
            log.error("AI评分调用失败: {}", response.getBody());
            return AIGradingResult.fail("调用失败");
            
        } catch (Exception e) {
            log.error("AI评分调用异常", e);
            return AIGradingResult.fail(e.getMessage());
        }
    }
    
    /**
     * 批量评分
     * @param answers 待评分的答案列表
     * @return 批量评分结果
     */
    public List<AIGradingResult> gradeBatch(List<StudentAnswer> answers) {
        List<AIGradingResult> results = new ArrayList<>();
        
        try {
            String url = pythonServiceUrl + "/grade/batch";
            
            // 构建批量请求参数
            List<Map<String, Object>> answerList = new ArrayList<>();
            
            for (StudentAnswer answer : answers) {
                // 查询标准答案
                Question question = questionMapper.selectById(answer.getQuestionId());
                if (question == null) {
                    log.warn("题目不存在，跳过评分: {}", answer.getQuestionId());
                    continue;
                }
                
                Map<String, Object> item = new HashMap<>();
                item.put("answerId", answer.getId());
                item.put("questionId", answer.getQuestionId());
                item.put("standardAnswer", question.getReferenceAnswer());
                item.put("studentAnswer", answer.getAnswerText());
                item.put("maxScore", answer.getMaxScore());
                
                answerList.add(item);
            }
            
            if (answerList.isEmpty()) {
                log.info("没有需要AI评分的题目");
                return results;
            }
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("answers", answerList);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            log.info("开始批量AI评分，共{}道题", answerList.size());
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject jsonResponse = JSON.parseObject(response.getBody());
                
                if (jsonResponse.getBoolean("success")) {
                    JSONArray dataArray = jsonResponse.getJSONArray("data");
                    
                    for (int i = 0; i < dataArray.size(); i++) {
                        JSONObject item = dataArray.getJSONObject(i);
                        
                        AIGradingResult result = new AIGradingResult();
                        result.setAnswerId(item.getInteger("answerId"));
                        result.setQuestionId(item.getInteger("questionId"));
                        result.setScore(item.getInteger("score"));
                        result.setMaxScore(item.getInteger("maxScore"));
                        result.setSimilarity(item.getDouble("similarity"));
                        result.setSuccess(true);
                        
                        results.add(result);
                        
                        // 更新数据库
                        updateAnswerScore(result);
                    }
                    
                    log.info("批量AI评分完成，共{}道题", results.size());
                }
            }
            
        } catch (Exception e) {
            log.error("批量AI评分异常", e);
        }
        
        return results;
    }
    
    /**
     * 更新答案评分
     */
    private void updateAnswerScore(AIGradingResult result) {
        try {
            if (result.getAnswerId() != null) {
                studentAnswerMapper.updateAiScore(
                    result.getAnswerId(),
                    result.getScore(),
                    result.getSimilarity(),
                    "ai_graded"
                );
            }
        } catch (Exception e) {
            log.error("更新答案评分失败", e);
        }
    }
    
    /**
     * AI评分结果类
     */
    public static class AIGradingResult {
        private Integer answerId;
        private Integer questionId;
        private Integer score;
        private Integer maxScore;
        private Double similarity;
        private boolean success;
        private String message;
        
        public static AIGradingResult fail(String message) {
            AIGradingResult result = new AIGradingResult();
            result.setSuccess(false);
            result.setMessage(message);
            return result;
        }
        
        // Getters and Setters
        public Integer getAnswerId() {
            return answerId;
        }
        
        public void setAnswerId(Integer answerId) {
            this.answerId = answerId;
        }
        
        public Integer getQuestionId() {
            return questionId;
        }
        
        public void setQuestionId(Integer questionId) {
            this.questionId = questionId;
        }
        
        public Integer getScore() {
            return score;
        }
        
        public void setScore(Integer score) {
            this.score = score;
        }
        
        public Integer getMaxScore() {
            return maxScore;
        }
        
        public void setMaxScore(Integer maxScore) {
            this.maxScore = maxScore;
        }
        
        public Double getSimilarity() {
            return similarity;
        }
        
        public void setSimilarity(Double similarity) {
            this.similarity = similarity;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
}
