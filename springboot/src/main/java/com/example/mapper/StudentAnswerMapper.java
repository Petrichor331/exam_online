package com.example.mapper;

import com.example.entity.StudentAnswer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 学生答题Mapper
 */
@Mapper
public interface StudentAnswerMapper {
    
    /**
     * 插入学生答案
     */
    int insert(StudentAnswer answer);
    
    /**
     * 批量插入答案
     */
    int batchInsert(@Param("list") List<StudentAnswer> answers);
    
    /**
     * 根据考试记录ID查询所有答案
     */
    List<StudentAnswer> selectByScoreId(@Param("scoreId") Integer scoreId);
    
    /**
     * 查询待评分的简答题
     */
    List<StudentAnswer> selectPendingSubjective(@Param("scoreId") Integer scoreId);
    
    /**
     * 更新AI评分
     */
    int updateAiScore(@Param("id") Integer id, 
                      @Param("aiScore") Integer aiScore,
                      @Param("similarity") Double similarity,
                      @Param("gradingStatus") String gradingStatus);
    
    /**
     * 更新最终成绩（人工审核）
     */
    int updateFinalScore(@Param("id") Integer id,
                         @Param("finalScore") Integer finalScore,
                         @Param("gradingStatus") String gradingStatus);
    
    /**
     * 更新评分人
     */
    int updateGradedBy(@Param("id") Integer id, @Param("gradedBy") Integer gradedBy);
    
    /**
     * 根据ID更新答案记录
     */
    int updateById(StudentAnswer answer);
}