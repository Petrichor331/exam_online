package com.example.controller;

import com.example.common.Result;
import com.example.common.dto.SaveAnswerDTO;
import com.example.common.dto.StartExamDTO;
import com.example.common.vo.ExamListVO;
import com.example.common.vo.HomeDataVO;
import com.example.common.vo.StartExamVO;
import com.example.entity.Account;
import com.example.entity.Course;
import com.example.entity.Score;
import com.example.service.ExamService;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 考试Controller
 */
@Slf4j
@RestController
@RequestMapping("/exam")
public class ExamController {
    
    @Resource
    private ExamService examService;
    
    /**
     * 获取首页数据
     * 返回：进行中的考试、待考科目、最近成绩
     */
    @GetMapping("/home")
    public Result getHomeData() {
        try {
            Account currentUser = TokenUtils.getCurrentUser();
            if (currentUser == null || !"STUDENT".equalsIgnoreCase(currentUser.getRole())) {
                return Result.error("请先登录学生账号");
            }
            
            HomeDataVO homeData = examService.getHomeData(currentUser.getId());
            return Result.success(homeData);
        } catch (Exception e) {
            log.error("获取首页数据失败", e);
            return Result.error("获取数据失败：" + e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") Integer pageNum,
                       @RequestParam(defaultValue = "10") Integer pageSize,
                       @RequestParam(required = false) String name) {
        try {
            Account currentUser = TokenUtils.getCurrentUser();
            if (currentUser == null || !"STUDENT".equalsIgnoreCase(currentUser.getRole())) {
                return Result.error("请先登录学生账号");
            }
            PageInfo<ExamListVO> pageInfo = examService.getExamList(pageNum, pageSize, name, currentUser.getId());
            return Result.success(pageInfo);
        } catch (Exception e) {
            log.error("获取考试列表失败", e);
            return Result.error("获取数据失败：" + e.getMessage());
        }
    }
    /**
     * 开始考试
     */
    @PostMapping("/start")
    public Result startExam(@RequestBody StartExamDTO dto) {
        try {
            Account currentUser = TokenUtils.getCurrentUser();
            if (currentUser == null || !"STUDENT".equalsIgnoreCase(currentUser.getRole())) {
                return Result.error("请先登录学生账号");
            }

            StartExamVO startExamVO = examService.startExam(currentUser.getId(), dto.getPaperId());
            return Result.success(startExamVO);
        } catch (Exception e) {
            log.error("开始考试失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 批量保存答案（交卷时调用）
     */
    @PostMapping("/save-answers")
    public Result saveAnswers(@RequestBody SaveAnswerDTO dto) {
        try {
            Account currentUser = TokenUtils.getCurrentUser();
            if (currentUser == null || !"STUDENT".equalsIgnoreCase(currentUser.getRole())) {
                return Result.error("请先登录学生账号");
            }
            
            examService.saveAnswers(currentUser.getId(), dto.getPaperId(), dto);
            return Result.success();
        } catch (Exception e) {
            log.error("保存答案失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 提交试卷
     */
    @PostMapping("/submit/{scoreId}")
    public Result submitExam(@PathVariable Integer scoreId) {
        try {
            Account currentUser = TokenUtils.getCurrentUser();
            if (currentUser == null || !"STUDENT".equalsIgnoreCase(currentUser.getRole())) {
                return Result.error("请先登录学生账号");
            }
            
            examService.submitExam(scoreId);
            return Result.success("试卷提交成功，正在评分中...");
        } catch (Exception e) {
            log.error("提交试卷失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 查看答卷
     */
    @GetMapping("/answer/{scoreId}")
    public Result getAnswer(@PathVariable Integer scoreId) {
        try {
            Account currentUser = TokenUtils.getCurrentUser();
            if (currentUser == null || !"STUDENT".equalsIgnoreCase(currentUser.getRole())) {
                return Result.error("请先登录学生账号");
            }
            
            Map<String, Object> answerData = examService.getAnswer(scoreId, currentUser.getId());
            return Result.success(answerData);
        } catch (Exception e) {
            log.error("查看答卷失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 模拟练习 - 生成随机试卷
     */
    @PostMapping("/practice")
    public Result practiceExam(@RequestParam Integer courseId,
                               @RequestParam(required = false) Integer difficulty,
                               @RequestParam(required = false) List<String> knowledgePoints) {
        try {
            Account currentUser = TokenUtils.getCurrentUser();
            if (currentUser == null || !"STUDENT".equalsIgnoreCase(currentUser.getRole())) {
                return Result.error("请先登录学生账号");
            }
            
            Map<String, Object> result = examService.createPracticeExam(currentUser.getId(), courseId, difficulty, knowledgePoints);
            return Result.success(result);
        } catch (Exception e) {
            log.error("创建模拟考试失败", e);
            return Result.error(e.getMessage());
        }
    }
}