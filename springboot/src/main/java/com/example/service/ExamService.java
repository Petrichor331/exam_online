package com.example.service;

import com.example.common.dto.QuestionOptionDTO;
import com.example.common.vo.ExamQuestionVO;
import com.example.common.vo.HomeDataVO;
import com.example.common.dto.SaveAnswerDTO;
import com.example.common.dto.StartExamDTO;
import com.example.common.vo.StartExamVO;
import com.example.common.vo.TestPaperVO;
import com.example.entity.*;
import com.example.mapper.*;
import com.example.utils.TokenUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 考试服务
 */
@Slf4j
@Service
public class ExamService {
    
    @Resource
    private ScoreMapper scoreMapper;
    
    @Resource
    private StudentAnswerMapper studentAnswerMapper;
    
    @Resource
    private TestPaperMapper testPaperMapper;
    
    @Resource
    private QuestionMapper questionMapper;
    
    @Resource
    private StudentCourseMapper studentCourseMapper;

    @Resource
    private QuestionTypeMapper questionTypeMapper;
    @Autowired
    private QuestionOptionMapper questionOptionMapper;
    
    @Autowired
    private PythonGradingService pythonGradingService;

    /**
     * 获取首页数据
     */
    public HomeDataVO getHomeData(Integer studentId) {
        HomeDataVO vo = new HomeDataVO();
        
        // 1. 进行中的考试
        vo.setOngoingExams(getOngoingExams(studentId));
        
        // 2. 待考科目（未开始且未参加的考试）
        vo.setUpcomingExams(getUpcomingExams(studentId));
        
        // 3. 最近成绩
        vo.setRecentScores(getRecentScores(studentId));
        
        return vo;
    }
    
    /**
     * 获取进行中的考试
     */
    private List<HomeDataVO.OngoingExamVO> getOngoingExams(Integer studentId) {
        List<Score> ongoingScores = scoreMapper.selectOngoingByStudentId(studentId);
        
        return ongoingScores.stream().map(score -> {
            HomeDataVO.OngoingExamVO vo = new HomeDataVO.OngoingExamVO();
            vo.setId(score.getId());
            vo.setPaperId(score.getPaperId());
            
            // 查询试卷信息
            TestPaper paper = testPaperMapper.selectById(score.getPaperId());
            if (paper != null) {
                vo.setName(paper.getName());
                // 计算题目数量
                String questionIds = paper.getQuestionIds();
                if (questionIds != null && !questionIds.isEmpty()) {
                    vo.setQuestionCount(questionIds.split(",").length);
                } else {
                    vo.setQuestionCount(0);
                }
                
                // 计算剩余时间（秒）
                LocalDateTime endTime = LocalDateTime.parse(paper.getEnd(), 
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                long remainingSeconds = java.time.Duration.between(LocalDateTime.now(), endTime).getSeconds();
                vo.setRemainingSeconds((int) Math.max(0, remainingSeconds));
                
                vo.setTotalScore(100); // 默认总分
            }
            
            return vo;
        }).collect(Collectors.toList());
    }
    
    /**
     * 获取待考科目（基于选课）
     * 只返回学生已选课程下的、未参加的、进行中的考试
     */
    private List<HomeDataVO.UpcomingExamVO> getUpcomingExams(Integer studentId) {
        // 1. 查询学生选的所有课程ID
        List<Integer> courseIds = studentCourseMapper.selectCourseIdsByStudentId(studentId);
        
        if (courseIds.isEmpty()) {
            return new ArrayList<>(); // 没有选课，返回空列表
        }
        
        // 2. 查询这些课程下的所有试卷
        List<TestPaperVO> allPapers = testPaperMapper.selectAll(new TestPaper());
        
        return allPapers.stream()
            // 只保留学生已选课程的试卷
            .filter(paper -> courseIds.contains(paper.getCourseId()))
            // 检查学生是否已参加
            .filter(paper -> {
                int count = scoreMapper.countByStudentAndPaper(studentId, paper.getId());
                return count == 0; // 未参加
            })
            // 检查是否未开始或进行中（未结束）
            .filter(paper -> {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime end = LocalDateTime.parse(paper.getEndTime(), 
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                return now.isBefore(end); // 未结束
            })
            // 转换为VO
            .map(paper -> {
                HomeDataVO.UpcomingExamVO vo = new HomeDataVO.UpcomingExamVO();
                vo.setId(paper.getId());
                vo.setName(paper.getPaperName());
                vo.setStartTime(paper.getStartTime());
                return vo;
            })
            .limit(5) // 最多显示5个
            .collect(Collectors.toList());
    }
    
    /**
     * 获取最近成绩
     */
    private List<HomeDataVO.RecentScoreVO> getRecentScores(Integer studentId) {
        List<Score> recentScores = scoreMapper.selectRecentByStudentId(studentId, 5);
        
        return recentScores.stream().map(score -> {
            HomeDataVO.RecentScoreVO vo = new HomeDataVO.RecentScoreVO();
            vo.setId(score.getId());
            
            // 查询试卷信息
            TestPaper paper = testPaperMapper.selectById(score.getPaperId());
            if (paper != null) {
                vo.setExamName(paper.getName());
            }
            
            vo.setScore(score.getTotalScore() != null ? score.getTotalScore().doubleValue() : 0.0);
            vo.setTotalScore(100); // 默认总分
            vo.setSubmitTime(score.getSubmitTime());
            
            return vo;
        }).collect(Collectors.toList());
    }
    
    /**
     * 开始考试
     */
    @Transactional
    public StartExamVO startExam(Integer studentId, Integer paperId) {
        // 检查是否已参加
        int count = scoreMapper.countByStudentAndPaper(studentId, paperId);
        if (count > 0) {
            throw new RuntimeException("您已参加过该考试");
        }

        // 创建score记录
        Score score = new Score();
        score.setStudentId(studentId);
        score.setPaperId(paperId);
        score.setStatus("ongoing");
        scoreMapper.insert(score);

        // 创建考试记录
        StartExamVO startExamVO = new StartExamVO();
        startExamVO.setScoreId(score.getId());
        startExamVO.setPaperId(paperId);
        TestPaper testPaper = testPaperMapper.selectById(paperId);
        startExamVO.setPaperName(testPaper.getName());
        startExamVO.setTime(testPaper.getTime());
        startExamVO.setEndTime(System.currentTimeMillis() + testPaper.getTime() * 60 * 1000);

        List<ExamQuestionVO> questions = new ArrayList<>();
        String[] questionIds = testPaper.getQuestionIds().split(",");
        for(String questionId : questionIds){
            Integer qId = Integer.parseInt(questionId);
            Question question = questionMapper.selectById(qId);
            ExamQuestionVO examQuestionVO = new ExamQuestionVO();
            examQuestionVO.setId(question.getId());
            examQuestionVO.setName(question.getName());
            examQuestionVO.setTypeId(question.getTypeId());
            examQuestionVO.setScore(question.getScore());
            QuestionType questionType = questionTypeMapper.selectById(question.getTypeId());
            examQuestionVO.setTypeName(questionType.getName());
            if(question.getTypeId() == 1 || question.getTypeId() == 2){
                List<QuestionOptionDTO> options = questionOptionMapper.selectOptionsForExam(question.getId());
                examQuestionVO.setOptions(options);
            }else{
                examQuestionVO.setOptions(null);
            }
            questions.add(examQuestionVO);
        }
        // 按题型ID排序：单选(1) -> 多选(2) -> 判断(3) -> 填空(4) -> 简答(5)
        questions.sort(Comparator.comparing(ExamQuestionVO::getTypeId));
        startExamVO.setQuestions(questions);
        return startExamVO;
    }
    
    /**
     * 批量保存答案（交卷时调用）
     */
    @Transactional
    public void saveAnswers(Integer studentId, Integer paperId, SaveAnswerDTO dto) {
        // 查询考试记录
        List<Score> scores = scoreMapper.selectByStudentId(studentId);
        Score score = scores.stream()
            .filter(s -> s.getPaperId().equals(paperId) && "ongoing".equals(s.getStatus()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("未找到进行中的考试"));
        
        // 查询试卷信息
        TestPaper paper = testPaperMapper.selectById(paperId);
        String[] questionIdArray = paper.getQuestionIds().split(",");
        
        // 构建答案列表
        List<StudentAnswer> answers = new ArrayList<>();
        
        for (SaveAnswerDTO.AnswerItem item : dto.getAnswers()) {
            StudentAnswer answer = new StudentAnswer();
            answer.setStudentId(studentId);
            answer.setPaperId(paperId);
            answer.setQuestionId(item.getQuestionId());
            
            // 查询题目信息
            Question question = questionMapper.selectById(item.getQuestionId());
            if (question != null) {
                answer.setMaxScore(question.getScore());
                
                // 根据题型存储到不同字段
                // 简答题(5)和填空题(4)保存到answer_text
                // 单选(1)、多选(2)、判断(3)保存到answer_option
                if (question.getTypeId() == 4 || question.getTypeId() == 5) { 
                    answer.setAnswerText(item.getAnswer());
                    log.info("保存文本答案，题目ID: {}, 题型: {}, 答案: {}", 
                        question.getId(), question.getTypeId(), item.getAnswer());
                } else {
                    answer.setAnswerOption(item.getAnswer());
                    log.info("保存选项答案，题目ID: {}, 题型: {}, 答案: {}", 
                        question.getId(), question.getTypeId(), item.getAnswer());
                }
            }
            
            answer.setGradingStatus("pending");
            answers.add(answer);
        }
        
        // 批量插入
        if (!answers.isEmpty()) {
            int result = studentAnswerMapper.batchInsert(answers);
            log.info("保存答案完成，学生ID: {}, 试卷ID: {}, 保存数量: {}, 插入结果: {}", 
                studentId, paperId, answers.size(), result);
        }
    }
    
    /**
     * 提交试卷
     */
    @Transactional
    public void submitExam(Integer scoreId) {
        // 1. 查询考试记录
        Score score = scoreMapper.selectById(scoreId);
        if (score == null) {
            throw new RuntimeException("考试记录不存在");
        }
        if (!"ongoing".equals(score.getStatus())) {
            throw new RuntimeException("考试已提交");
        }

        // 2. 保存提交时间
        score.setSubmitTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        scoreMapper.updateById(score);

        // 3. 客观题自动评分
        autoGradeObjectiveQuestions(scoreId);

        // 4. 更新考试状态为评分中
        scoreMapper.updateStatus(scoreId, "grading");

        // 5. 异步调用AI评分（简答题）
        callPythonGradingAsync(scoreId);
    }

    /**
     * 客观题自动评分
     * 策略：
     * - 单选、多选、判断：自动评分，状态graded（不可修改）
     * - 填空题：自动评分，状态ai_graded（老师可修改）
     * - 简答题：AI评分，状态ai_graded（老师可修改）
     */
    private void autoGradeObjectiveQuestions(Integer scoreId) {
        // 查询该考试的所有答案
        List<StudentAnswer> answers = studentAnswerMapper.selectByScoreId(scoreId);
        log.info("开始客观题评分，scoreId: {}, 找到答案数量: {}", scoreId, answers.size());

        for (StudentAnswer answer : answers) {
            log.info("评分题目ID: {}, answerId: {}, 当前状态: {}, 学生答案: {}", 
                answer.getQuestionId(), answer.getId(), answer.getGradingStatus(), answer.getAnswerOption());
            // 查询题目信息
            Question question = questionMapper.selectById(answer.getQuestionId());
            if (question == null) continue;

            // 单选、多选、判断（纯客观题）
            if (question.getTypeId() == 1 || question.getTypeId() == 2 || question.getTypeId() == 3) {
                String correctAnswer;
                
                // 判断题的正确答案在 question.reference_answer 字段
                if (question.getTypeId() == 3) {
                    correctAnswer = question.getReferenceAnswer();
                } else {
                    // 单选、多选题的正确答案在 question_option 表
                    List<QuestionOption> options = questionOptionMapper.selectByQuestionId(question.getId());
                    correctAnswer = options.stream()
                            .filter(QuestionOption::getIsCorrect)
                            .map(QuestionOption::getOptionLabel)
                            .sorted()
                            .collect(Collectors.joining(","));
                }

                // 获取学生答案
                String studentAnswer = answer.getAnswerOption();
                
                log.info("客观题评分，题目ID: {}, 题型: {}, 学生答案: {}, 正确答案: {}", 
                    question.getId(), question.getTypeId(), studentAnswer, correctAnswer);

                // 对比答案（去除空格，不区分大小写）
                if (studentAnswer != null && correctAnswer != null && 
                    studentAnswer.trim().equalsIgnoreCase(correctAnswer.trim())) {
                    answer.setFinalScore(question.getScore());
                } else {
                    answer.setFinalScore(0);
                }
                answer.setGradingStatus("graded");

                // 更新答案记录
                log.info("准备更新客观题，answerId: {}, finalScore: {}, gradingStatus: {}", 
                    answer.getId(), answer.getFinalScore(), answer.getGradingStatus());
                int result = studentAnswerMapper.updateById(answer);
                log.info("客观题评分完成，answerId: {}, 题目ID: {}, 得分: {}, 状态: {}, 更新结果: {}", 
                    answer.getId(), answer.getQuestionId(), answer.getFinalScore(), answer.getGradingStatus(), result);
                if (result == 0) {
                    log.error("更新失败！answerId: {}, 请检查数据库", answer.getId());
                }
            }
            // 填空题：不自动评分，保持pending状态，等待教师手动评分
            else if (question.getTypeId() == 4) {
                // 填空题保持pending状态，由教师手动评分
                log.info("填空题暂不自动评分，题目ID: {}, 保持pending状态，等待教师手动评分", answer.getQuestionId());
            }
            // 简答题：保持pending，等待AI评分
            else {
                log.info("简答题暂不评分，题目ID: {}, 保持pending状态", answer.getQuestionId());
            }
        }
    }
    
    /**
     * 异步调用Python AI评分
     */
    @Async
    public void callPythonGradingAsync(Integer scoreId) {
        try {
            log.info("开始AI评分，scoreId: {}", scoreId);
            
            // 查询待评分的简答题
            List<StudentAnswer> subjectiveAnswers = studentAnswerMapper.selectPendingSubjective(scoreId);
            
            if (subjectiveAnswers.isEmpty()) {
                log.info("没有需要AI评分的题目，scoreId: {}", scoreId);
                calculateTotalScore(scoreId);
                return;
            }
            
            // 调用Python AI评分服务进行批量评分
            List<PythonGradingService.AIGradingResult> results = pythonGradingService.gradeBatch(subjectiveAnswers);
            
            log.info("AI评分完成，共{}道题，scoreId: {}", results.size(), scoreId);
            
            // 计算总分
            calculateTotalScore(scoreId);
            
        } catch (Exception e) {
            log.error("AI评分失败，scoreId: {}", scoreId, e);
        }
    }
    
    /**
     * 计算总分
     */
    private void calculateTotalScore(Integer scoreId) {
        // 查询所有答案
        List<StudentAnswer> answers = studentAnswerMapper.selectByScoreId(scoreId);

        int totalScore = 0;
        int gradedCount = 0;

        for (StudentAnswer answer : answers) {
            // 优先使用finalScore（人工评分或客观题自动评分），否则使用aiScore（AI评分）
            Integer scoreValue = answer.getFinalScore();
            if (scoreValue == null) {
                scoreValue = answer.getAiScore();
            }
            if (scoreValue != null) {
                totalScore += scoreValue;
                gradedCount++;
            }
        }

        // 更新总分
        scoreMapper.updateTotalScore(scoreId, totalScore);

        // 如果所有题目都已评分，更新状态为已完成
        if (gradedCount == answers.size()) {
            scoreMapper.updateStatus(scoreId, "finished");
            log.info("考试评分完成，scoreId: {}, 总分: {}", scoreId, totalScore);
        } else {
            log.info("考试部分评分，scoreId: {}, 已评分: {}/{}, 当前总分: {}",
                    scoreId, gradedCount, answers.size(), totalScore);
        }
    }
}