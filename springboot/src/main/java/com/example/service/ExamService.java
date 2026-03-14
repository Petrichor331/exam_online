package com.example.service;

import com.alibaba.fastjson.JSON;
import com.example.common.dto.QuestionOptionDTO;
import com.example.common.vo.*;
import com.example.common.dto.SaveAnswerDTO;
import com.example.common.dto.StartExamDTO;
import com.example.entity.*;
import com.example.mapper.*;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
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
    private TeacherMapper teacherMapper;

    @Resource
    private QuestionTypeMapper questionTypeMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private QuestionOptionMapper questionOptionMapper;
    
    @Autowired
    private PythonGradingService pythonGradingService;
    
    @Autowired
    private TestPaperService testPaperService;

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
     * 获取进行中的考试(详细）
     */
    private List<HomeDataVO.OngoingExamVO> getOngoingExams(Integer studentId) {
        List<Score> ongoingScores = scoreMapper.selectOngoingByStudentId(studentId);
        
        List<HomeDataVO.OngoingExamVO> result = ongoingScores.stream().map(score -> {
            HomeDataVO.OngoingExamVO vo = new HomeDataVO.OngoingExamVO();
            vo.setId(score.getId());
            vo.setPaperId(score.getPaperId());
            
            // 查询试卷信息
            TestPaper paper = testPaperMapper.selectById(score.getPaperId());
            if (paper == null) {
                return null;  // 试卷不存在，跳过
            }
            
            // 过滤掉模拟练习
            if ("practice".equals(paper.getType())) {
                return null;
            }
            
            vo.setName(paper.getName());
            // 计算题目数量
            String questionIds = paper.getQuestionIds();
            if (questionIds != null && !questionIds.isEmpty()) {
                vo.setQuestionCount(questionIds.split(",").length);
            } else {
                vo.setQuestionCount(0);
            }
            
            // 从 Redis 读取考试开始时间
            String startKey = "exam:start:" + studentId + ":" + paper.getId();
            Object startObj = redisTemplate.opsForValue().get(startKey);
            
            long examStartTime = 0;
            if (startObj != null) {
                examStartTime = Long.parseLong(startObj.toString());
            }
            
            // 如果没有开始时间，使用当前时间
            if (examStartTime == 0) {
                examStartTime = System.currentTimeMillis();
            }
            
            // 计算考试时长结束时间
            long examDurationEndTime = examStartTime + paper.getTime() * 60 * 1000;
            
            // 试卷设置的结束时间
            Long paperEndTime = null;
            if (paper.getEnd() != null && !paper.getEnd().isEmpty()) {
                try {
                    paperEndTime = java.time.LocalDateTime.parse(paper.getEnd(), 
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                } catch (Exception e) {
                    log.warn("解析试卷结束时间失败: {}", paper.getEnd());
                }
            }
            
            // 取两个时间中较早的作为最终结束时间
            long finalEndTime = examDurationEndTime;
            if (paperEndTime != null && paperEndTime < examDurationEndTime) {
                finalEndTime = paperEndTime;
            }
            
            // 计算剩余时间（秒）
            long remainingSeconds = (finalEndTime - System.currentTimeMillis()) / 1000;
            
            // 如果考试已过期，不再显示在"进行中"
            if (remainingSeconds <= 0) {
                return null;
            }
            
            vo.setRemainingSeconds((int) remainingSeconds);
            vo.setTotalScore(100); // 默认总分
            
            Integer teacherId = paper.getTeacherId();
            Teacher teacher = teacherMapper.selectById(teacherId);
            vo.setTeacherName(teacher.getName());
            
            return vo;
        }).filter(vo -> vo != null).collect(Collectors.toList());
        
        return result;
    }
    /**
     * 获取考试列表（含状态判断）
     */
    public PageInfo<ExamListVO> getExamList(Integer pageNum, Integer pageSize, String name, Integer studentId) {
        PageHelper.startPage(pageNum, pageSize);
        
        // 获取学生已参加的考试和待参加的考试
        List<ExamListVO> voList = getExamListByStudent(studentId, name);
        
        PageInfo<ExamListVO> pageInfo = new PageInfo<>(voList);
        pageInfo.setPageNum(pageNum);
        pageInfo.setPageSize(pageSize);
        
        return pageInfo;
    }
    
    /**
     * 获取学生的考试列表（含状态）
     */
    private List<ExamListVO> getExamListByStudent(Integer studentId, String name) {
        List<ExamListVO> result = new ArrayList<>();
        
        // 1. 获取已交卷/已完成的学生答卷记录 (从score表)
        List<Score> scoreList = scoreMapper.selectByStudentId(studentId);
        
        // 2. 获取学生已选的课程对应的试卷(待考试)
        List<StudentCourse> studentCourses = studentCourseMapper.selectByStudentId(studentId);
        
        // 处理已完成的考试记录
        for (Score score : scoreList) {
            TestPaper paper = testPaperMapper.selectById(score.getPaperId());
            if (paper == null) continue;
            
            // 过滤掉模拟练习
            if ("practice".equals(paper.getType())) {
                continue;
            }
            
            // 搜索过滤
            if (name != null && !name.isEmpty() && !paper.getName().contains(name)) {
                continue;
            }
            
            ExamListVO vo = new ExamListVO();
            vo.setId(paper.getId());
            vo.setPaperId(paper.getId());
            vo.setPaperName(paper.getName());
            vo.setQuestionCount(paper.getQuestionIds() != null ? paper.getQuestionIds().split(",").length : 0);
            vo.setTotalScore(100);
            vo.setStartTime(paper.getStart());
            vo.setEndTime(paper.getEnd());
            vo.setScoreId(score.getId());
            vo.setExamScore(score.getTotalScore());
            
            // 获取教师名称
            Teacher teacher = teacherMapper.selectById(paper.getTeacherId());
            vo.setTeacherName(teacher != null ? teacher.getName() : "");
            
            // 判断状态：pending-待考试, ongoing-进行中, waiting-待评分, finished-已完成
            if (score.getTotalScore() != null) {
                vo.setStatus("finished"); // 已评分
            } else if (score.getSubmitTime() != null) {
                vo.setStatus("grading"); // 已交卷，待评分
            } else {
                // 未交卷，判断时间
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime startTime = LocalDateTime.parse(paper.getStart(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                LocalDateTime endTime = LocalDateTime.parse(paper.getEnd(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                
                if (now.isBefore(startTime)) {
                    vo.setStatus("pending"); // 未开始
                } else if (now.isAfter(endTime)) {
                    vo.setStatus("grading"); // 已过期，自动交卷
                } else {
                    vo.setStatus("ongoing"); // 进行中
                }
            }
            
            result.add(vo);
        }
        
        // 3. 处理待考试的试卷（学生已选课程但未参加考试）
        for (StudentCourse sc : studentCourses) {
            // 获取该课程下的所有试卷
            List<TestPaper> papers = testPaperMapper.selectByCourseIdList(sc.getCourseId());
            
            for (TestPaper paper : papers) {
                // 过滤掉模拟练习
                if ("practice".equals(paper.getType())) {
                    continue;
                }
                
                // 搜索过滤
                if (name != null && !name.isEmpty() && !paper.getName().contains(name)) {
                    continue;
                }

                // 检查是否已在scoreList中
                boolean exists = result.stream().anyMatch(v -> v.getPaperId().equals(paper.getId()));
                if (exists) continue;
            
                ExamListVO vo = new ExamListVO();
                vo.setId(paper.getId());
                vo.setPaperId(paper.getId());
                vo.setPaperName(paper.getName());
                vo.setQuestionCount(paper.getQuestionIds() != null ? paper.getQuestionIds().split(",").length : 0);
                vo.setTotalScore(100);
                vo.setStartTime(paper.getStart());
                vo.setEndTime(paper.getEnd());
                
                // 获取教师名称
                Teacher teacher = teacherMapper.selectById(paper.getTeacherId());
                vo.setTeacherName(teacher != null ? teacher.getName() : "");
                
                // 判断状态
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime startTime = LocalDateTime.parse(paper.getStart(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                LocalDateTime endTime = LocalDateTime.parse(paper.getEnd(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                
                if (now.isBefore(startTime)) {
                    vo.setStatus("pending"); // 待考试
                } else if (now.isAfter(endTime)) {
                    vo.setStatus("grading"); // 已过期
                } else {
                    vo.setStatus("ongoing"); // 进行中
                }
                
                result.add(vo);
            }
        }
        
        // 按状态排序：进行中 > 待考试 > 待评分 > 已完成
        result.sort((a, b) -> {
            int orderA = getStatusOrder(a.getStatus());
            int orderB = getStatusOrder(b.getStatus());
            return Integer.compare(orderA, orderB);
        });
        
        return result;
    }
    
    private int getStatusOrder(String status) {
        switch (status) {
            case "ongoing": return 1;
            case "pending": return 2;
            case "grading": return 3;
            case "finished": return 4;
            default: return 5;
        }
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
            // 只保留非模拟练习的试卷
            .filter(paper -> !"practice".equals(paper.getType()))
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
            if (paper == null) {
                return null;  // 试卷已删除，跳过
            }
            
            // 过滤掉模拟练习
            if ("practice".equals(paper.getType())) {
                return null;
            }
            vo.setExamName(paper.getName());
            
            // 设置状态
            vo.setStatus(score.getStatus());
            
            // 只有已完成(finished)才显示分数，否则显示待批改
            if ("finished".equals(score.getStatus())) {
                vo.setScore(score.getTotalScore() != null ? score.getTotalScore().doubleValue() : 0.0);
            } else {
                vo.setScore(null);  // 待批改状态不显示分数
            }
            
            vo.setTotalScore(100); // 默认总分
            vo.setSubmitTime(score.getSubmitTime());
            
            return vo;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
    
    /**
     * 开始考试
     */
    @Transactional
    public StartExamVO startExam(Integer studentId, Integer paperId) {
        // 检查是否已参加
        int count = scoreMapper.countByStudentAndPaper(studentId, paperId);
        if (count > 0) {
            Score existingScore = scoreMapper.selectByPaperIdAndStudentId(paperId, studentId);
            if (existingScore != null) {
                // 从 Redis 读取开始时间
                String startKey = "exam:start:" + studentId + ":" + paperId;
                Object startObj = redisTemplate.opsForValue().get(startKey);
                log.info("读取Redis key: {}, 值: {}", startKey, startObj);
                
                long examStartTime = startObj != null ? Long.parseLong(startObj.toString()) : System.currentTimeMillis();
                log.info("计算examStartTime: {}", examStartTime);

                TestPaper testPaper = testPaperMapper.selectById(paperId);
                long examDurationEndTime = examStartTime + testPaper.getTime() * 60 * 1000;
                log.info("计算examDurationEndTime: {}, 当前时间: {}", examDurationEndTime, System.currentTimeMillis());

                return buildStartExamVO(existingScore, testPaper, examDurationEndTime);
            }
            throw new RuntimeException("您已参加过该考试");
        }

        // 首次开始，保存开始时间到 Redis
        long examStartTime = System.currentTimeMillis();
        String startKey = "exam:start:" + studentId + ":" + paperId;
        redisTemplate.opsForValue().set(startKey, examStartTime, 24, TimeUnit.HOURS);
        log.info("保存Redis key: {}, 值: {}", startKey, examStartTime);

        // 创建 score 记录
        Score score = new Score();
        score.setStudentId(studentId);
        score.setPaperId(paperId);
        score.setStatus("ongoing");
        scoreMapper.insert(score);

        TestPaper testPaper = testPaperMapper.selectById(paperId);
        long examDurationEndTime = examStartTime + testPaper.getTime() * 60 * 1000;

        return buildStartExamVO(score, testPaper, examDurationEndTime);
    }

    // 用于已有 score 记录（重新进入考试）
    private StartExamVO buildStartExamVO(Score score, TestPaper testPaper, long examDurationEndTime) {
        StartExamVO startExamVO = new StartExamVO();
        startExamVO.setScoreId(score.getId());
        startExamVO.setPaperId(testPaper.getId());
        startExamVO.setPaperName(testPaper.getName());
        startExamVO.setTime(testPaper.getTime());
        
        // 试卷设置的结束时间
        Long paperEndTime = null;
        if (testPaper.getEnd() != null && !testPaper.getEnd().isEmpty()) {
            try {
                paperEndTime = java.time.LocalDateTime.parse(testPaper.getEnd(), 
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (Exception e) {
                log.warn("解析试卷结束时间失败: {}", testPaper.getEnd());
            }
        }
        startExamVO.setPaperEndTime(paperEndTime);

        // 取两个时间中较早的作为最终结束时间
        if (paperEndTime != null && paperEndTime < examDurationEndTime) {
            startExamVO.setEndTime(paperEndTime);
        } else {
            startExamVO.setEndTime(examDurationEndTime);
        }

        List<ExamQuestionVO> questions = new ArrayList<>();
        String[] questionIds = testPaper.getQuestionIds().split(",");
        for (String questionId : questionIds) {
            Integer qId = Integer.parseInt(questionId);
            Question question = questionMapper.selectById(qId);
            ExamQuestionVO examQuestionVO = new ExamQuestionVO();
            examQuestionVO.setId(question.getId());
            examQuestionVO.setName(question.getName());
            examQuestionVO.setTypeId(question.getTypeId());
            examQuestionVO.setScore(question.getScore());
            QuestionType questionType = questionTypeMapper.selectById(question.getTypeId());
            examQuestionVO.setTypeName(questionType.getName());
            if (question.getTypeId() == 1 || question.getTypeId() == 2) {
                List<QuestionOptionDTO> options = questionOptionMapper.selectOptionsForExam(question.getId());
                examQuestionVO.setOptions(options);
            } else {
                examQuestionVO.setOptions(null);
            }
            questions.add(examQuestionVO);
        }
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

        // 4. 更新考试状态为待评分（不立即显示分数，等教师批改）
        scoreMapper.updateStatus(scoreId, "grading");

        // 5. 异步调用AI评分（简答题）
        callPythonGradingAsync(scoreId);

        // 6. 删除 Redis 中的开始时间
        String startKey = "exam:start:" + score.getStudentId() + ":" + score.getPaperId();
        redisTemplate.delete(startKey);
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
     * 计算总分（AI评分完成后调用，但不离线更新到数据库）
     * 等教师确认后才真正更新总分
     */
    private void calculateTotalScore(Integer scoreId) {
        // 查询所有答案
        List<StudentAnswer> answers = studentAnswerMapper.selectByScoreId(scoreId);

        int totalScore = 0;
        int gradedCount = 0;

        for (StudentAnswer answer : answers) {
            // 优先使用finalScore（人工评分），否则使用aiScore（AI评分）
            Integer scoreValue = answer.getFinalScore();
            if (scoreValue == null) {
                scoreValue = answer.getAiScore();
            }
            if (scoreValue != null) {
                totalScore += scoreValue;
                gradedCount++;
            }
        }

        // AI评分完成后不立即更新总分，保持"waiting"状态
        // 等教师确认评分后才更新总分和状态
        log.info("AI评分完成，scoreId: {}, 已评分: {}/{}, 等待教师确认",
                scoreId, gradedCount, answers.size());
    }
    
    /**
     * 查看答卷
     */
    public Map<String, Object> getAnswer(Integer scoreId, Integer studentId) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取成绩记录
        Score score = scoreMapper.selectById(scoreId);
        if (score == null || !score.getStudentId().equals(studentId)) {
            throw new RuntimeException("无权查看此答卷");
        }
        
        // 获取试卷信息
        TestPaper paper = testPaperMapper.selectById(score.getPaperId());
        if (paper == null) {
            throw new RuntimeException("试卷不存在");
        }
        
        // 构建考试信息
        Map<String, Object> examInfo = new HashMap<>();
        examInfo.put("paperName", paper.getName());
        Teacher teacher = teacherMapper.selectById(paper.getTeacherId());
        examInfo.put("teacherName", teacher != null ? teacher.getName() : "");
        examInfo.put("startTime", paper.getStart());
        examInfo.put("score", score.getTotalScore());
        examInfo.put("totalScore", 100);
        
        result.put("examInfo", examInfo);
        
        // 获取题目列表
        List<Map<String, Object>> questionList = new ArrayList<>();
        
        // 解析题目ID
        String[] questionIds = paper.getQuestionIds().split(",");
        
        for (String qId : questionIds) {
            Question question = questionMapper.selectById(Integer.parseInt(qId.trim()));
            if (question == null) continue;
            
            Map<String, Object> qInfo = new HashMap<>();
            qInfo.put("id", question.getId());
            qInfo.put("name", question.getName());
            qInfo.put("typeId", question.getTypeId());
            // 获取题型名称
            QuestionType questionType = questionTypeMapper.selectById(question.getTypeId());
            qInfo.put("typeName", questionType != null ? questionType.getName() : "");
            qInfo.put("score", question.getScore());
            
            // 获取学生答案
            StudentAnswer studentAnswer = studentAnswerMapper.selectByStudentAndPaperAndQuestion(studentId, score.getPaperId(), question.getId());
            if (studentAnswer != null) {
                qInfo.put("studentAnswer", studentAnswer.getAnswerOption());
                qInfo.put("isCorrect", studentAnswer.getFinalScore() != null && studentAnswer.getFinalScore() > 0);
            } else {
                qInfo.put("studentAnswer", null);
                qInfo.put("isCorrect", null);
            }
            
            // 获取标准答案
            String correctAnswer = "";
            // 客观题：单选、多选、判断 - 从选项中获取正确答案
            if (question.getTypeId() != null && question.getTypeId() <= 3) {
                List<QuestionOption> options = questionOptionMapper.selectByQuestionId(question.getId());
                correctAnswer = options.stream()
                    .filter(QuestionOption::getIsCorrect)
                    .map(QuestionOption::getOptionLabel)
                    .sorted()
                    .collect(Collectors.joining(","));
            } else {
                // 主观题：从题目表中获取参考答案
                correctAnswer = question.getReferenceAnswer();
            }
            qInfo.put("correctAnswer", correctAnswer);
            
            // 获取选项
            List<QuestionOption> options = questionOptionMapper.selectByQuestionId(question.getId());
            List<Map<String, Object>> optionList = new ArrayList<>();
            for (QuestionOption opt : options) {
                Map<String, Object> optInfo = new HashMap<>();
                optInfo.put("optionLabel", opt.getOptionLabel());
                optInfo.put("optionContent", opt.getOptionContent());
                optInfo.put("isCorrect", opt.getIsCorrect());
                optionList.add(optInfo);
            }
            qInfo.put("options", optionList);
            
            questionList.add(qInfo);
        }
        
        result.put("questions", questionList);
        
        return result;
    }
    
    /**
     * 创建模拟练习试卷
     */
    @Transactional
    public Map<String, Object> createPracticeExam(Integer studentId, Integer courseId, Integer difficulty, List<String> knowledgePoints) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 调用TestPaperService的自动组卷功能
            String questionIds = testPaperService.autoGenerateQuestions(courseId, difficulty, knowledgePoints);
            
            // 2. 创建模拟试卷（练习模式，无时间限制）
            TestPaper practicePaper = new TestPaper();
            practicePaper.setName("模拟练习-" + System.currentTimeMillis());
            practicePaper.setCourseId(courseId);
            practicePaper.setTeacherId(1); // 系统自动生成
            practicePaper.setType("practice");
            practicePaper.setStart(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            practicePaper.setEnd("2099-12-31 23:59:59"); // 长期有效
            practicePaper.setTime(7200); // 2小时
            practicePaper.setQuestionIds(questionIds);
            testPaperMapper.insert(practicePaper);
            
            // 3. 创建考试记录
            Score score = new Score();
            score.setStudentId(studentId);
            score.setPaperId(practicePaper.getId());
            score.setTotalScore(100);
            score.setStatus("ongoing");
            scoreMapper.insert(score);
            
            result.put("paperId", practicePaper.getId());
            result.put("scoreId", score.getId());
            result.put("message", "模拟试卷生成成功");
            
            log.info("创建模拟练习成功，studentId: {}, courseId: {}, paperId: {}", studentId, courseId, practicePaper.getId());
            
        } catch (Exception e) {
            log.error("创建模拟练习失败", e);
            throw new RuntimeException("创建模拟练习失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 保存临时答案到redis
     */
    public void saveTempAnswer(Integer studentId, Integer paperId, SaveAnswerDTO dto){
        String key = "exam:temp:" + studentId + ":" + paperId;
        TestPaper testPaper = testPaperMapper.selectById(paperId);
        redisTemplate.opsForValue().set(key, dto, testPaper.getTime(), TimeUnit.MINUTES);

        log.info("保存临时答案成功，studentId: {}, paperId: {}", studentId, paperId);
    }

    public SaveAnswerDTO getTempAnswer(Integer studentId, Integer paperId){
        String key = "exam:temp:" + studentId + ":" + paperId;
        Object obj = redisTemplate.opsForValue().get(key);
        if(obj != null){
            //讲Object转回DTO，要先转成json再转成DTO
            String json = JSON.toJSONString(obj);
            return JSON.parseObject(json, SaveAnswerDTO.class);
        }
        return null;
    }
}