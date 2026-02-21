package com.example.controller;

import com.example.common.Result;
import com.example.common.vo.ScoreDetailVO;
import com.example.common.vo.ScoreListVO;
import com.example.service.ScoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 成绩管理/改卷Controller
 */
@Slf4j
@RestController
@RequestMapping("/score")
public class ScoreController {
    
    @Autowired
    private ScoreService scoreService;
    
    /**
     * 获取教师的课程列表（用于改卷时先选课程）
     * @param teacherId 教师ID
     * @return 课程列表（包含每个课程的答卷数量）
     */
    @GetMapping("/courses")
    public Result getTeacherCourses(@RequestParam Integer teacherId) {
        try {
            List<Map<String, Object>> courses = scoreService.getTeacherCourses(teacherId);
            return Result.success(courses);
        } catch (Exception e) {
            log.error("获取教师课程列表失败", e);
            return Result.error("500", "获取教师课程列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取答卷列表 - 支持按课程筛选
     * @param teacherId 教师ID（必需）
     * @param courseId 课程ID（可选，不传则查询所有课程）
     * @return 答卷列表
     */
    @GetMapping("/list")
    public Result list(@RequestParam Integer teacherId, 
                      @RequestParam(required = false) Integer courseId) {
        try {
            List<ScoreListVO> list = scoreService.selectAll(teacherId, courseId);
            return Result.success(list);
        } catch (Exception e) {
            log.error("获取答卷列表失败", e);
            return Result.error("500", "获取答卷列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取答卷详情（用于改卷）
     * @param scoreId 考试记录ID
     * @return 答卷详情
     */
    @GetMapping("/detail/{scoreId}")
    public Result detail(@PathVariable Integer scoreId) {
        try {
            ScoreDetailVO detail = scoreService.getScoreDetail(scoreId);
            if (detail == null) {
                return Result.error("404", "答卷不存在");
            }
            return Result.success(detail);
        } catch (Exception e) {
            log.error("获取答卷详情失败, scoreId: {}", scoreId, e);
            return Result.error("500", "获取答卷详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 人工改卷 - 修改单个题目分数
     * @param answerId 答案ID
     * @return 是否成功
     */
    @PostMapping("/grade/{answerId}")
    public Result grade(@PathVariable Integer answerId, @RequestBody Map<String, Object> params) {
        try {
            Integer score = (Integer) params.get("score");

            if (score == null) {
                return Result.error("400", "分数不能为空");
            }
            
            boolean success = scoreService.manualGrade(answerId, score);
            if (success) {
                return Result.success("改卷成功");
            } else {
                return Result.error("500", "改卷失败");
            }
        } catch (Exception e) {
            log.error("人工改卷失败, answerId: {}", answerId, e);
            return Result.error("500", "改卷失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量改卷
     * @param scoreId 考试记录ID
     * @return 是否成功
     */
    @PostMapping("/batch/{scoreId}")
    public Result batchGrade(@PathVariable Integer scoreId, @RequestBody Map<String, Object> params) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> scores = (List<Map<String, Object>>) params.get("scores");
            
            if (scores == null || scores.isEmpty()) {
                return Result.error("400", "分数列表不能为空");
            }
            
            boolean success = scoreService.batchManualGrade(scoreId, scores);
            if (success) {
                return Result.success("批量改卷成功");
            } else {
                return Result.error("500", "批量改卷失败");
            }
        } catch (Exception e) {
            log.error("批量改卷失败, scoreId: {}", scoreId, e);
            return Result.error("500", "批量改卷失败: " + e.getMessage());
        }
    }
    
    /**
     * 确认AI评分（接受AI评分结果）
     * @param scoreId 考试记录ID
     * @return 是否成功
     */
    @PostMapping("/confirm-ai/{scoreId}")
    public Result confirmAiGrading(@PathVariable Integer scoreId) {
        try {
            boolean success = scoreService.confirmAiGrading(scoreId);
            if (success) {
                return Result.success("AI评分已确认");
            } else {
                return Result.error("500", "确认AI评分失败");
            }
        } catch (Exception e) {
            log.error("确认AI评分失败, scoreId: {}", scoreId, e);
            return Result.error("500", "确认AI评分失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取成绩统计
     * @param paperId 试卷ID
     * @return 统计信息
     */
    @GetMapping("/statistics")
    public Result statistics(@RequestParam Integer paperId) {
        try {
            Map<String, Object> stats = scoreService.getScoreStatistics(paperId);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取成绩统计失败, paperId: {}", paperId, e);
            return Result.error("500", "获取成绩统计失败: " + e.getMessage());
        }
    }
}
