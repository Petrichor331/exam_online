package com.example.service;


import cn.hutool.core.date.DateUtil;
import com.example.common.Result;
import com.example.common.dto.TestPaperAddDTO;
import com.example.common.vo.TestPaperVO;
import com.example.entity.Account;
import com.example.entity.Question;
import com.example.entity.TestPaper;
import com.example.exception.CustomException;
import com.example.mapper.QuestionMapper;
import com.example.mapper.ScoreMapper;
import com.example.mapper.TestPaperMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TestPaperService {
    @Resource
    private TestPaperMapper testPaperMapper;

    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private ScoreMapper scoreMapper;

    private static final Logger log = LoggerFactory.getLogger(TestPaperService.class);

    public Result add(TestPaperAddDTO testPaperAddDTO) {
        //首先参数校验
        if(ObjectUtils.isEmpty(testPaperAddDTO.getCourseId())){
            return Result.error("请选择课程");
        }
        if(ObjectUtils.isEmpty(testPaperAddDTO.getPaperName())){
            return Result.error("请输入试卷名称");
        }
        if(ObjectUtils.isEmpty(testPaperAddDTO.getStartTime()) || ObjectUtils.isEmpty(testPaperAddDTO.getEndTime())){
            return Result.error("请选择考试时间");
        }
        //根据出卷方式生成id
        String questionIds;
        if("manual".equals(testPaperAddDTO.getGenerateType())){
            //手动组卷：使用前端传来的题目ID列表
            List<Integer> questionIdList = testPaperAddDTO.getQuestionIds();
            if(ObjectUtils.isEmpty(questionIdList)){
                throw new CustomException("手动组卷需要至少选择一道题目");
            }
            questionIds = questionIdList.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
        }
        else if("auto".equals(testPaperAddDTO.getGenerateType())){
            //自动组卷：使用遗传算法
            questionIds = autoGenerateQuestions(
                testPaperAddDTO.getCourseId(),
                testPaperAddDTO.getDifficulty(),
                testPaperAddDTO.getKnowledgePoints()
            );
        }
        else {
            throw new CustomException("无效的出卷方式");
        }

        TestPaper testPaper = new TestPaper();
        testPaper.setCourseId(testPaperAddDTO.getCourseId());
        testPaper.setName(testPaperAddDTO.getPaperName());
        testPaper.setStart(testPaperAddDTO.getStartTime());
        testPaper.setEnd(testPaperAddDTO.getEndTime());
        testPaper.setTime(testPaperAddDTO.getDuration());
        testPaper.setQuestionIds(questionIds);
        testPaper.setType(testPaperAddDTO.getGenerateType());

        Account account = TokenUtils.getCurrentUser();
        System.out.println("当前用户: " + account);
        if (account != null) {
            System.out.println("用户角色: " + account.getRole());
            if ("TEACHER".equalsIgnoreCase(account.getRole())) {
                testPaper.setTeacherId(account.getId());
                System.out.println("设置教师ID: " + account.getId());
            }
        }
        testPaperMapper.insert(testPaper);
        return Result.success();

    }
    /**
     * 固定题型配置：题型代码 -> (数量, 分值)
     * 1-单选, 2-多选, 3-判断, 4-填空, 5-简答
     */
    private static final Map<Integer, int[]> FIXED_TYPE_CONFIG = new HashMap<>();
    static {
        FIXED_TYPE_CONFIG.put(1, new int[]{10, 3});   // 单选：10题，每题3分
        FIXED_TYPE_CONFIG.put(2, new int[]{4, 5});    // 多选：4题，每题5分
        FIXED_TYPE_CONFIG.put(3, new int[]{9, 2});    // 判断：9题，每题2分
        FIXED_TYPE_CONFIG.put(4, new int[]{4, 3});    // 填空：4题，每题3分
        FIXED_TYPE_CONFIG.put(5, new int[]{2, 10});   // 简答：2题，每题10分
    }
    
    /**
     * 自动组卷 - 基于遗传算法，固定题型数量（内部方法）
     */
    private String autoGenerateQuestionsInternal(Integer courseId, Integer difficulty, List<String> knowledgePoints) {
        // 1. 获取候选题库（带分值信息）
        List<Question> candidates = questionMapper.selectByCourseId(courseId);
        if (candidates.size() < 29) {
            throw new CustomException("题库题目数量不足，无法自动组卷");
        }
        
        // 按题型分类题目
        Map<Integer, List<Question>> questionsByType = candidates.stream()
                .collect(Collectors.groupingBy(Question::getTypeId));
        
        // 检查各题型数量是否足够
        for (Map.Entry<Integer, int[]> entry : FIXED_TYPE_CONFIG.entrySet()) {
            int typeId = entry.getKey();
            int requiredCount = entry.getValue()[0];
            List<Question> typeQuestions = questionsByType.getOrDefault(typeId, new ArrayList<>());
            if (typeQuestions.size() < requiredCount) {
                throw new CustomException("题库中" + getTypeName(typeId) + "数量不足，需要" + requiredCount + "题，实际只有" + typeQuestions.size() + "题");
            }
        }
        
        // 目标总分
        final int TARGET_SCORE = 100;
        
        // 2. 遗传算法参数
        int populationSize = 50;      // 种群大小
        int maxGenerations = 100;     // 最大迭代次数
        double crossoverRate = 0.8;   // 交叉概率
        double mutationRate = 0.1;    // 变异概率
        double eliteRate = 0.1;       // 精英保留比例
        
        // 3. 初始化种群（按固定题型数量）
        List<PaperChromosome> population = initializePopulationWithFixedTypes(questionsByType, populationSize);
        
        // 4. 迭代进化
        PaperChromosome bestSolution = null;
        double bestFitness = 0;
        
        for (int generation = 0; generation < maxGenerations; generation++) {
            // 计算适应度
            for (PaperChromosome paper : population) {
                double fitness = calculateFitnessWithFixedTypes(paper, difficulty, knowledgePoints, TARGET_SCORE);
                paper.setFitness(fitness);
                
                if (fitness > bestFitness) {
                    bestFitness = fitness;
                    bestSolution = paper;
                }
            }
            
            // 如果找到满分方案，提前结束
            if (bestFitness >= 0.99) {
                log.info("遗传算法提前收敛，第{}代，适应度：{}", generation, bestFitness);
                break;
            }
            
            // 选择、交叉、变异
            population = selection(population, eliteRate);
            crossoverWithFixedTypes(population, crossoverRate, questionsByType);
            mutationWithFixedTypes(population, mutationRate, questionsByType);
        }
        
        if (bestSolution == null) {
            throw new CustomException("自动组卷失败，未找到合适方案");
        }
        
        // 验证结果
        validateResult(bestSolution);
        
        // 返回最优解
        return bestSolution.getQuestionIds().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
    
    /**
     * 获取题型名称
     */
    private String getTypeName(int typeId) {
        switch (typeId) {
            case 1: return "单选题";
            case 2: return "多选题";
            case 3: return "判断题";
            case 4: return "填空题";
            case 5: return "简答题";
            default: return "未知题型";
        }
    }
    
    /**
     * 验证组卷结果
     */
    private void validateResult(PaperChromosome paper) {
        Map<Integer, Long> typeCount = paper.getQuestions().stream()
                .collect(Collectors.groupingBy(Question::getTypeId, Collectors.counting()));
        
        StringBuilder sb = new StringBuilder("组卷结果：");
        for (Map.Entry<Integer, int[]> entry : FIXED_TYPE_CONFIG.entrySet()) {
            int typeId = entry.getKey();
            int requiredCount = entry.getValue()[0];
            int score = entry.getValue()[1];
            long actualCount = typeCount.getOrDefault(typeId, 0L);
            sb.append(String.format("%s:%d题(%d分) ", getTypeName(typeId), actualCount, actualCount * score));
        }
        sb.append("总分：").append(paper.getTotalScore()).append("分");
        log.info(sb.toString());
    }
    
    /**
     * 试卷染色体（个体）
     */
    private static class PaperChromosome {
        private List<Integer> questionIds;
        private List<Question> questions;
        private double fitness;
        private int totalScore;

        public PaperChromosome(List<Question> questions) {
            this.questions = questions;
            this.questionIds = questions.stream()
                    .map(Question::getId)
                    .collect(Collectors.toList());
            this.totalScore = calculateFixedTotalScore( questions);
        }
        private int calculateFixedTotalScore(List<Question> questions) {
            Map<Integer, Long> typeCount = questions.stream()
                    .collect(Collectors.groupingBy(Question::getTypeId, Collectors.counting()));

            int score = 0;
            for (Map.Entry<Integer, int[]> entry : FIXED_TYPE_CONFIG.entrySet()) {
                int typeId = entry.getKey();
                int countPerType = entry.getValue()[0];
                int scorePerQuestion = entry.getValue()[1];
                long actualCount = typeCount.getOrDefault(typeId, 0L);
                score += actualCount * scorePerQuestion;
            }
            return score;
        }
        
        public void setQuestions(List<Question> questions) {
            this.questions = questions;
            this.questionIds = questions.stream()
                    .map(Question::getId)
                    .collect(Collectors.toList());
            this.totalScore = questions.stream()
                    .mapToInt(Question::getScore)
                    .sum();
        }
        
        // Getters and setters
        public List<Integer> getQuestionIds() { return questionIds; }
        public void setQuestionIds(List<Integer> questionIds) { this.questionIds = questionIds; }
        public List<Question> getQuestions() { return questions; }
        public double getFitness() { return fitness; }
        public void setFitness(double fitness) { this.fitness = fitness; }
        public int getTotalScore() { return totalScore; }
    }
    
    /**
     * 初始化种群 - 按固定题型数量选择
     */
    private List<PaperChromosome> initializePopulationWithFixedTypes(Map<Integer, List<Question>> questionsByType, int populationSize) {
        List<PaperChromosome> population = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < populationSize; i++) {
            List<Question> selected = new ArrayList<>();
            
            // 按固定配置从每种题型中选择指定数量
            for (Map.Entry<Integer, int[]> entry : FIXED_TYPE_CONFIG.entrySet()) {
                int typeId = entry.getKey();
                int requiredCount = entry.getValue()[0];
                
                List<Question> typeQuestions = new ArrayList<>(questionsByType.getOrDefault(typeId, new ArrayList<>()));
                Collections.shuffle(typeQuestions, random);
                
                // 选择指定数量的题目
                for (int j = 0; j < requiredCount && j < typeQuestions.size(); j++) {
                    selected.add(typeQuestions.get(j));
                }
            }
            
            population.add(new PaperChromosome(selected));
        }
        
        return population;
    }
    
    /**
     * 计算适应度 - 固定题型数量
     */
    /**
     * 改进的适应度计算 - 知识点覆盖更精细
     */
    private double calculateFitnessWithFixedTypes(PaperChromosome paper, Integer targetDifficulty,
                                                  List<String> targetKnowledgePoints, int targetScore) {
        List<Question> questions = paper.getQuestions();

        // 1. 题型数量匹配度（权重35%）- 硬约束
        double typeFitness = calculateTypeMatchScore(questions);

        // 2. 总分匹配度（权重25%）- 硬约束
        double scoreFitness = (paper.getTotalScore() == targetScore) ? 1.0 : 0.0;

        // 3. 难度匹配度（权重20%）- 改进：分段匹配
        double difficultyFitness = calculateDifficultyScore(questions, targetDifficulty);

        // 4. 知识点覆盖率（权重20%）- 改进：权重覆盖+分布均匀性
        double knowledgeFitness = calculateKnowledgeScore(questions, targetKnowledgePoints);

        return typeFitness * 0.35 + scoreFitness * 0.25 +
                difficultyFitness * 0.20 + knowledgeFitness * 0.20;
    }

    /**
     * 题型匹配 - 严格匹配得1分，否则按差距扣分
     */
    private double calculateTypeMatchScore(List<Question> questions) {
        Map<Integer, Long> typeCount = questions.stream()
                .collect(Collectors.groupingBy(Question::getTypeId, Collectors.counting()));

        double score = 0;
        for (Map.Entry<Integer, int[]> entry : FIXED_TYPE_CONFIG.entrySet()) {
            int required = entry.getValue()[0];
            long actual = typeCount.getOrDefault(entry.getKey(), 0L);
            int diff = (int) Math.abs(actual - required);
            score += Math.max(0, 1.0 - diff * 0.5);  // 差1题扣0.5，差2题不得分
        }
        return score / 5.0;
    }

    /**
     * 难度匹配 - 不仅看平均，还看分布
     */
    private double calculateDifficultyScore(List<Question> questions, int targetDifficulty) {
        // 平均难度匹配
        double avgDifficulty = questions.stream()
                .mapToInt(Question::getDifficulty)
                .average()
                .orElse(3.0);
        double avgScore = 1.0 - Math.abs(avgDifficulty - targetDifficulty) / 4.0;

        // 难度分布均匀性（标准差）
        double variance = questions.stream()
                .mapToDouble(q -> Math.pow(q.getDifficulty() - avgDifficulty, 2))
                .average()
                .orElse(0);
        double stdDev = Math.sqrt(variance);
        double uniformScore = 1.0 / (1.0 + stdDev);  // 标准差越小越好

        return avgScore * 0.7 + uniformScore * 0.3;
    }

    /**
     * 知识点覆盖 - 加权覆盖+避免重复
     */
    private double calculateKnowledgeScore(List<Question> questions, List<String> targetPoints) {
        if (targetPoints == null || targetPoints.isEmpty()) return 1.0;

        // 统计各知识点出现次数
        Map<String, Long> pointCount = questions.stream()
                .map(Question::getKnowledgePoint)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()));

        // 加权覆盖：重要知识点权重更高
        double weightedCoverage = 0;
        double totalWeight = 0;
        for (int i = 0; i < targetPoints.size(); i++) {
            String point = targetPoints.get(i);
            double weight = (targetPoints.size() - i) / (double) targetPoints.size();  // 靠前的权重高
            totalWeight += weight;
            if (pointCount.containsKey(point)) {
                weightedCoverage += weight;
            }
        }

        // 重复度惩罚：同一知识点出现多次扣分
        long duplicates = pointCount.values().stream()
                .filter(count -> count > 1)
                .mapToLong(count -> count - 1)
                .sum();
        double duplicatePenalty = Math.min(0.3, duplicates * 0.05);

        return (weightedCoverage / totalWeight) - duplicatePenalty;
    }
    /**
     * 选择操作（轮盘赌选择 + 精英保留）
     */
    private List<PaperChromosome> selection(List<PaperChromosome> population, double eliteRate) {
        List<PaperChromosome> newPopulation = new ArrayList<>();
        
        // 精英保留
        int eliteCount = (int) (population.size() * eliteRate);
        population.sort((a, b) -> Double.compare(b.getFitness(), a.getFitness()));
        newPopulation.addAll(population.subList(0, eliteCount));
        
        // 轮盘赌选择
        double totalFitness = population.stream()
                .mapToDouble(PaperChromosome::getFitness)
                .sum();
        
        Random random = new Random();
        while (newPopulation.size() < population.size()) {
            double point = random.nextDouble() * totalFitness;
            double cumulative = 0;
            
            for (PaperChromosome paper : population) {
                cumulative += paper.getFitness();
                if (cumulative >= point) {
                    newPopulation.add(new PaperChromosome(new ArrayList<>(paper.getQuestions())));
                    break;
                }
            }
        }
        
        return newPopulation;
    }
    
    /**
     * 交叉操作（按题型交叉）- 保持固定题型数量
     */
    private void crossoverWithFixedTypes(List<PaperChromosome> population, double crossoverRate,
                                         Map<Integer, List<Question>> questionsByType) {
        Random random = new Random();
        
        for (int i = 0; i < population.size() - 1; i += 2) {
            if (random.nextDouble() > crossoverRate) continue;
            
            PaperChromosome parent1 = population.get(i);
            PaperChromosome parent2 = population.get(i + 1);
            
            // 按题型分组
            Map<Integer, List<Question>> typeQuestions1 = parent1.getQuestions().stream()
                    .collect(Collectors.groupingBy(Question::getTypeId));
            Map<Integer, List<Question>> typeQuestions2 = parent2.getQuestions().stream()
                    .collect(Collectors.groupingBy(Question::getTypeId));
            
            List<Question> child1 = new ArrayList<>();
            List<Question> child2 = new ArrayList<>();
            
            // 对每种题型进行单点交叉
            for (Integer typeId : FIXED_TYPE_CONFIG.keySet()) {
                List<Question> type1 = new ArrayList<>(typeQuestions1.getOrDefault(typeId, new ArrayList<>()));
                List<Question> type2 = new ArrayList<>(typeQuestions2.getOrDefault(typeId, new ArrayList<>()));
                
                int requiredCount = FIXED_TYPE_CONFIG.get(typeId)[0];
                
                if (type1.size() >= 2 && type2.size() >= 2) {
                    int point = random.nextInt(Math.min(type1.size(), type2.size()) - 1) + 1;
                    
                    List<Question> childType1 = new ArrayList<>(type1.subList(0, point));
                    childType1.addAll(type2.subList(point, type2.size()));
                    
                    List<Question> childType2 = new ArrayList<>(type2.subList(0, point));
                    childType2.addAll(type1.subList(point, type1.size()));
                    
                    // 去重并确保数量正确
                    childType1 = adjustTypeQuestions(removeDuplicates(childType1), questionsByType.get(typeId), requiredCount);
                    childType2 = adjustTypeQuestions(removeDuplicates(childType2), questionsByType.get(typeId), requiredCount);
                    
                    child1.addAll(childType1);
                    child2.addAll(childType2);
                } else {
                    // 数量不够，保持不变
                    child1.addAll(type1);
                    child2.addAll(type2);
                }
            }
            
            population.set(i, new PaperChromosome(child1));
            population.set(i + 1, new PaperChromosome(child2));
        }
    }
    
    /**
     * 调整某类题型的题目数量
     */
    private List<Question> adjustTypeQuestions(List<Question> questions, List<Question> candidates, int requiredCount) {
        List<Question> adjusted = new ArrayList<>(questions);
        Random random = new Random();
        
        // 如果数量过多，随机删除
        while (adjusted.size() > requiredCount) {
            adjusted.remove(random.nextInt(adjusted.size()));
        }
        
        // 如果数量不足，从候选中补充
        Set<Integer> usedIds = adjusted.stream().map(Question::getId).collect(Collectors.toSet());
        List<Question> available = candidates.stream()
                .filter(q -> !usedIds.contains(q.getId()))
                .collect(Collectors.toList());
        
        while (adjusted.size() < requiredCount && !available.isEmpty()) {
            Question q = available.get(random.nextInt(available.size()));
            adjusted.add(q);
            available.remove(q);
        }
        
        return adjusted;
    }
    
    /**
     * 变异操作（同题型替换）- 保持固定题型数量
     */
    private void mutationWithFixedTypes(List<PaperChromosome> population, double mutationRate,
                                        Map<Integer, List<Question>> questionsByType) {
        Random random = new Random();
        
        for (PaperChromosome paper : population) {
            if (random.nextDouble() > mutationRate) continue;
            
            // 随机选择一种题型进行变异
            List<Integer> typeIds = new ArrayList<>(FIXED_TYPE_CONFIG.keySet());
            int mutateTypeId = typeIds.get(random.nextInt(typeIds.size()));
            
            List<Question> questions = new ArrayList<>(paper.getQuestions());
            List<Question> typeQuestions = questions.stream()
                    .filter(q -> q.getTypeId().equals(mutateTypeId))
                    .collect(Collectors.toList());
            
            if (typeQuestions.isEmpty()) continue;
            
            // 随机选择该题型的一道题目进行替换
            int mutateIndex = random.nextInt(typeQuestions.size());
            Question oldQuestion = typeQuestions.get(mutateIndex);
            
            // 从该题型的候选题库中选择一道不同的题目
            List<Question> candidates = questionsByType.getOrDefault(mutateTypeId, new ArrayList<>());
            List<Question> available = candidates.stream()
                    .filter(q -> !questions.contains(q))
                    .collect(Collectors.toList());
            
            if (!available.isEmpty()) {
                Question newQuestion = available.get(random.nextInt(available.size()));
                
                // 替换
                int indexInAll = questions.indexOf(oldQuestion);
                if (indexInAll >= 0) {
                    questions.set(indexInAll, newQuestion);
                }
            }
            
            paper.setQuestions(questions);
        }
    }
    
    /**
     * 去重
     */
    private List<Question> removeDuplicates(List<Question> questions) {
        Set<Integer> seen = new HashSet<>();
        List<Question> unique = new ArrayList<>();
        
        for (Question q : questions) {
            if (!seen.contains(q.getId())) {
                seen.add(q.getId());
                unique.add(q);
            }
        }
        
        return unique;
    }
    
    public PageInfo<TestPaperVO> selectPage(TestPaper testPaper, Integer pageNum, Integer pageSize) {
        Account account = TokenUtils.getCurrentUser();
        if(account.getRole().equals("TEACHER")){
            Integer teacherId = account.getId();
            testPaper.setTeacherId(teacherId);
        }
        PageHelper.startPage(pageNum, pageSize);
        List<TestPaperVO> list = testPaperMapper.selectAll(testPaper);
        return PageInfo.of(list);
    }

    public List<TestPaperVO> selectAll(TestPaper testPaper) {
        return testPaperMapper.selectAll(testPaper);
    }

    public TestPaper selectById(Integer id) {
        return testPaperMapper.selectById(id);
    }

    public void updateById(TestPaper testPaper) {
        // 检查试卷是否已有学生参加考试
        if (hasExamRecords(testPaper.getId())) {
            throw new CustomException("该试卷已有学生参加考试，禁止修改");
        }
        // 检查试卷是否已结束
        TestPaper existing = testPaperMapper.selectById(testPaper.getId());
        if (existing != null && existing.getEnd() != null) {
            java.time.LocalDateTime endTime = DateUtil.parseLocalDateTime(existing.getEnd());
            if (endTime.isBefore(java.time.LocalDateTime.now())) {
                throw new CustomException("该试卷已结束，禁止修改");
            }
        }
        testPaperMapper.updateById(testPaper);
    }

    /**
     * 检查试卷是否已有学生参加考试
     */
    public boolean hasExamRecords(Integer paperId) {
        if (paperId == null) {
            return false;
        }
        return scoreMapper.countByPaperId(paperId) > 0;
    }

    public void deleteById(Integer id) {
        testPaperMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            testPaperMapper.deleteById(id);
        }
    }
    
    /**
     * 公开的自动组卷方法，供模拟练习调用
     */
    public String autoGenerateQuestions(Integer courseId, Integer difficulty, List<String> knowledgePoints) {
        return autoGenerateQuestionsInternal(courseId, difficulty, knowledgePoints);
    }

}


