package com.example.service;

import com.example.common.vo.ScoreDetailVO;
import com.example.common.vo.ScoreListVO;
import com.example.entity.*;
import com.example.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 成绩管理/改卷服务
 */
@Slf4j
@Service
public class ScoreService {
    
    @Autowired
    private ScoreMapper scoreMapper;
    
    @Autowired
    private StudentAnswerMapper studentAnswerMapper;
    
    @Autowired
    private TestPaperMapper testPaperMapper;
    
    @Autowired
    private QuestionMapper questionMapper;
    
    @Autowired
    private QuestionOptionMapper questionOptionMapper;
    
    @Autowired
    private StudentMapper studentMapper;
    
    @Autowired
    private CourseMapper courseMapper;
    
    @Autowired
    private ExamPlanMapper examPlanMapper;
    
    /**
     * 获取教师教授的课程列表（有考试的课程）
     * @param teacherId 教师ID
     * @return 课程列表
     */
    public List<Map<String, Object>> getTeacherCourses(Integer teacherId) {
        // 查询该教师的所有课程
        List<Course> courses = courseMapper.selectByTeacherId(teacherId);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Course course : courses) {
            Map<String, Object> map = new HashMap<>();
            map.put("courseId", course.getId());
            map.put("courseName", course.getName());
            map.put("credit", course.getCredit());
            
            // 统计该课程下的答卷数量
            int examCount = scoreMapper.countByTeacherAndCourse(teacherId, course.getId());
            map.put("examCount", examCount);
            
            result.add(map);
        }
        
        return result;
    }
    
    /**
     * 获取答卷列表（教师查看）- 按课程筛选
     * @param teacherId 教师ID
     * @param courseId 课程ID（可选，为null则查所有课程）
     * @return 答卷列表
     */
    public List<ScoreListVO> selectAll(Integer teacherId, Integer courseId) {
        // 查询该教师课程下的考试答卷，可按课程筛选
        return scoreMapper.selectByTeacherIdAndCourseId(teacherId, courseId);
    }
    
    /**
     * 获取答卷详情（用于改卷）
     * @param scoreId 考试记录ID
     * @return 答卷详情
     */
    public ScoreDetailVO getScoreDetail(Integer scoreId) {
        Score score = scoreMapper.selectById(scoreId);
        if (score == null) {
            return null;
        }
        
        ScoreDetailVO vo = new ScoreDetailVO();
        vo.setScoreId(scoreId);
        vo.setStudentId(score.getStudentId());
        vo.setPaperId(score.getPaperId());
        vo.setTotalScore(score.getTotalScore());
        vo.setStatus(score.getStatus());
        
        // 查询学生信息
        Student student = studentMapper.selectById(score.getStudentId());
        if (student != null) {
            vo.setStudentName(student.getName());
        }
        
        // 查询试卷信息
        TestPaper paper = testPaperMapper.selectById(score.getPaperId());
        if (paper != null) {
            vo.setPaperName(paper.getName());
            
            // 查询课程信息
            Course course = courseMapper.selectById(paper.getCourseId());
            if (course != null) {
                vo.setCourseName(course.getName());
            }
        }
        
        // 查询所有答题详情
        List<StudentAnswer> answers = studentAnswerMapper.selectByScoreId(scoreId);
        List<ScoreDetailVO.AnswerDetail> answerDetails = new ArrayList<>();
        
        for (StudentAnswer answer : answers) {
            ScoreDetailVO.AnswerDetail detail = new ScoreDetailVO.AnswerDetail();
            detail.setAnswerId(answer.getId());
            detail.setQuestionId(answer.getQuestionId());
            detail.setMaxScore(answer.getMaxScore());
            detail.setAiScore(answer.getAiScore());
            detail.setFinalScore(answer.getFinalScore());
            detail.setGradingStatus(answer.getGradingStatus());
            
            // 查询题目信息
            Question question = questionMapper.selectById(answer.getQuestionId());
            if (question != null) {
                detail.setQuestionName(question.getName());
                detail.setQuestionTypeId(question.getTypeId());
                detail.setReferenceAnswer(question.getReferenceAnswer());
                
                // 根据题型获取学生答案
                if (question.getTypeId() == 5) { // 简答题
                    detail.setStudentAnswer(answer.getAnswerText());
                } else if (question.getTypeId() == 4) { // 填空题
                    detail.setStudentAnswer(answer.getAnswerText());
                } else { // 选择题
                    detail.setStudentAnswer(answer.getAnswerOption());
                    
                    // 查询标准答案
                    List<QuestionOption> options = questionOptionMapper.selectByQuestionId(question.getId());
                    String correctAnswer = options.stream()
                            .filter(QuestionOption::getIsCorrect)
                            .map(QuestionOption::getOptionLabel)
                            .sorted()
                            .collect(java.util.stream.Collectors.joining(","));
                    detail.setCorrectAnswer(correctAnswer);
                }
            }
            
            answerDetails.add(detail);
        }
        
        vo.setAnswers(answerDetails);
        return vo;
    }
    
    /**
     * 人工改卷 - 修改单个题目分数
     * @param answerId 答案ID
     * @param score 新分数
     * @return 是否成功
     */
    @Transactional
    public boolean manualGrade(Integer answerId, Integer score ) {
        try {
            StudentAnswer answer = studentAnswerMapper.selectById(answerId);
            if (answer == null) {
                log.error("答案不存在，answerId: {}", answerId);
                return false;
            }

            // 检查分数是否超过满分
            if (score > answer.getMaxScore()) {
                log.warn("评分超过满分，answerId: {}, score: {}, maxScore: {}", 
                    answerId, score, answer.getMaxScore());
                score = answer.getMaxScore();
            }
            
            // 更新最终分数和状态
            int result = studentAnswerMapper.updateFinalScore(answerId, score, "graded");
            
            if (result > 0) {
                // 重新计算总分
                Score this_score = scoreMapper.selectByPaperIdAndStudentId(answer.getPaperId(), answer.getStudentId());
                recalculateTotalScore(this_score.getId());
                log.info("人工改卷成功，answerId: {}, score: {}", answerId, score);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("人工改卷失败，answerId: {}", answerId, e);
            return false;
        }
    }
    
    /**
     * 批量改卷
     * @param scoreId 考试记录ID
     * @return 是否成功
     */
    @Transactional
    public boolean batchManualGrade(Integer scoreId, List<Map<String, Object>> scores) {
        try {
            Score score = scoreMapper.selectById(scoreId);
            if (score == null) {
                log.error("考试记录不存在，scoreId: {}", scoreId);
                return false;
            }
            
            int successCount = 0;
            for (Map<String, Object> item : scores) {
                Integer answerId = (Integer) item.get("answerId");
                // 分数可能是 Integer 或 Double，需要兼容处理
                Object scoreObj = item.get("score");
                Integer scoreValue = scoreObj != null ? ((Number) scoreObj).intValue() : null;

                if (scoreValue != null && manualGrade(answerId, scoreValue)) {
                    successCount++;
                }
            }
            
            // 重新计算总分
            recalculateTotalScore(scoreId);
            
            log.info("批量改卷完成，scoreId: {}, 成功: {}/{}", scoreId, successCount, scores.size());
            return successCount == scores.size();
            
        } catch (Exception e) {
            log.error("批量改卷失败，scoreId: {}", scoreId, e);
            return false;
        }
    }
    
    /**
     * 确认AI评分（接受AI评分结果作为最终分数）
     * @param scoreId 考试记录ID
     * @return 是否成功
     */
    @Transactional
    public boolean confirmAiGrading(Integer scoreId) {
        try {
            List<StudentAnswer> answers = studentAnswerMapper.selectByScoreId(scoreId);
            
            for (StudentAnswer answer : answers) {
                // 只处理AI评分状态的题目
                if ("ai_graded".equals(answer.getGradingStatus())) {
                    Integer aiScore = answer.getAiScore();
                    if (aiScore != null) {
                        studentAnswerMapper.updateFinalScore(
                            answer.getId(), 
                            aiScore, 
                            "graded"
                        );
                    }
                }
            }
            
            // 重新计算总分
            recalculateTotalScore(scoreId);
            
            // 更新考试状态为已完成
            scoreMapper.updateStatus(scoreId, "finished");
            
            log.info("AI评分已确认，scoreId: {}", scoreId);
            return true;
            
        } catch (Exception e) {
            log.error("确认AI评分失败，scoreId: {}", scoreId, e);
            return false;
        }
    }
    
    /**
     * 重新计算总分并更新状态为已完成
     * @param scoreId 考试记录ID
     */
    private void recalculateTotalScore(Integer scoreId) {
        List<StudentAnswer> answers = studentAnswerMapper.selectByScoreId(scoreId);
        
        int totalScore = 0;
        for (StudentAnswer answer : answers) {
            // 优先使用finalScore
            Integer score = answer.getFinalScore();
            if (score == null) {
                score = answer.getAiScore();
            }
            if (score != null) {
                totalScore += score;
            }
        }
        
        scoreMapper.updateTotalScore(scoreId, totalScore);
        
        // 更新状态为已完成，此时学生才能看到分数
        scoreMapper.updateStatus(scoreId, "finished");
        
        log.info("重新计算总分完成，scoreId: {}, totalScore: {}, 状态: finished", scoreId, totalScore);
    }
    
    /**
     * 获取成绩统计信息
     * @param paperId 试卷ID
     * @return 统计信息
     */
    public Map<String, Object> getScoreStatistics(Integer paperId) {
        List<Score> scores = scoreMapper.selectByPaperId(paperId);
        
        if (scores.isEmpty()) {
            return new HashMap<>();
        }
        
        // 计算统计信息
        int totalStudents = scores.size();
        int finishedCount = (int) scores.stream()
                .filter(s -> "finished".equals(s.getStatus()))
                .count();
        
        double maxScore = scores.stream()
                .filter(s -> s.getTotalScore() != null)
                .mapToInt(Score::getTotalScore)
                .max()
                .orElse(0);
                
        double minScore = scores.stream()
                .filter(s -> s.getTotalScore() != null)
                .mapToInt(Score::getTotalScore)
                .min()
                .orElse(0);
                
        double avgScore = scores.stream()
                .filter(s -> s.getTotalScore() != null)
                .mapToInt(Score::getTotalScore)
                .average()
                .orElse(0);
        
        // 及格率（假设60分及格）
        long passCount = scores.stream()
                .filter(s -> s.getTotalScore() != null && s.getTotalScore() >= 60)
                .count();
        double passRate = (double) passCount / finishedCount * 100;
        
        // 分数段分布
        int[] distribution = new int[5]; // 0-59, 60-69, 70-79, 80-89, 90-100
        for (Score score : scores) {
            if (score.getTotalScore() == null) continue;
            int s = score.getTotalScore();
            if (s < 60) distribution[0]++;
            else if (s < 70) distribution[1]++;
            else if (s < 80) distribution[2]++;
            else if (s < 90) distribution[3]++;
            else distribution[4]++;
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStudents", totalStudents);
        stats.put("finishedCount", finishedCount);
        stats.put("maxScore", maxScore);
        stats.put("minScore", minScore);
        stats.put("avgScore", String.format("%.2f", avgScore));
        stats.put("passRate", String.format("%.2f", passRate));
        stats.put("distribution", distribution);
        
        return stats;
    }

    /**
     * 获取个人所有成绩
     *
     * */

    public List<ScoreListVO> getMyScores(Integer studentId) {
        List<Score> scores = scoreMapper.selectByStudentId(studentId);
        List<ScoreListVO> scoreListVOS = new ArrayList<>();
        for (Score score : scores) {
            //查试卷信息
            TestPaper testPaper = testPaperMapper.selectById(score.getPaperId());
            
            // 试卷已删除则跳过
            if (testPaper == null) {
                continue;
            }
            
            //排除模拟练习
            if ("practice".equals(testPaper.getType())) {
                continue;
            }
            
            ScoreListVO scoreListVO = new ScoreListVO();
            scoreListVO.setScoreId(score.getId());
            scoreListVO.setStudentId(score.getStudentId());
            scoreListVO.setPaperId(score.getPaperId());
            scoreListVO.setTotalScore(score.getTotalScore());
            scoreListVO.setStatus(score.getStatus());
            scoreListVO.setSubmitTime(score.getSubmitTime());
            scoreListVO.setPaperName(testPaper.getName());
            scoreListVO.setCourseId(testPaper.getCourseId());
            //查课程名称
            Course course = courseMapper.selectById(testPaper.getCourseId());
            if (course != null) {
                scoreListVO.setCourseName(course.getName());
            }
            scoreListVOS.add(scoreListVO);
        }
        return scoreListVOS;
    }

    /**
     * 获取学生知识点掌握情况（只统计客观题：单选、多选、判断）
     */
    public List<Map<String, Object>> getKnowledgePointStats(Integer studentId, Integer courseId) {
        List<Score> scores = scoreMapper.selectByStudentId(studentId);
        Map<String, int[]> knowledgeStats = new HashMap<>();
        
        for (Score score : scores) {
            if (score.getStatus() == null || !"finished".equals(score.getStatus())) {
                continue;
            }
            
            TestPaper paper = testPaperMapper.selectById(score.getPaperId());
            //排除模拟练习
            if (paper == null || paper.getQuestionIds() == null || "practice".equals(paper.getType())) {
                continue;
            }
            
            // 按课程筛选
            if (courseId != null && !courseId.equals(paper.getCourseId())) {
                continue;
            }
            
            String[] questionIds = paper.getQuestionIds().split(",");
            for (String qId : questionIds) {
                try {
                    Question question = questionMapper.selectById(Integer.parseInt(qId.trim()));
                    if (question == null || question.getKnowledgePoint() == null) {
                        continue;
                    }
                    
                    // 只统计客观题：1-单选，2-多选，3-判断
                    if (question.getTypeId() == null || question.getTypeId() > 3) {
                        continue;
                    }
                    
                    String knowledgePoint = question.getKnowledgePoint();
                    knowledgeStats.putIfAbsent(knowledgePoint, new int[2]);
                    
                    StudentAnswer answer = studentAnswerMapper.selectByStudentAndPaperAndQuestion(
                        studentId, score.getPaperId(), question.getId());
                    
                    knowledgeStats.get(knowledgePoint)[0]++;
                    if (answer != null && answer.getFinalScore() != null && answer.getFinalScore() > 0) {
                        knowledgeStats.get(knowledgePoint)[1]++;
                    }
                } catch (Exception e) {
                    // skip invalid question
                }
            }
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : knowledgeStats.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", entry.getKey());
            item.put("total", entry.getValue()[0]);
            item.put("correct", entry.getValue()[1]);
            item.put("rate", entry.getValue()[0] > 0 ? 
                Math.round(entry.getValue()[1] * 100.0 / entry.getValue()[0]) : 0);
            result.add(item);
        }
        
        result.sort((a, b) -> Integer.compare((Integer)b.get("total"), (Integer)a.get("total")));
        return result;
    }
}
