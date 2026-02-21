package com.example.mapper;

import com.example.common.vo.ScoreListVO;
import com.example.entity.Score;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 考试记录Mapper
 */
@Mapper
public interface ScoreMapper {
    
    /**
     * 插入考试记录
     */
    int insert(Score score);
    
    /**
     * 根据ID查询
     */
    Score selectById(Integer id);
    
    /**
     * 查询所有
     */
    List<Score> selectAll(Score score);
    
    /**
     * 根据ID更新
     */
    void updateById(Score score);
    
    /**
     * 根据ID删除
     */
    void deleteById(Integer id);
    
    /**
     * 查询学生的考试记录
     */
    List<Score> selectByStudentId(@Param("studentId") Integer studentId);
    
    /**
     * 查询进行中的考试
     */
    List<Score> selectOngoingByStudentId(@Param("studentId") Integer studentId);
    
    /**
     * 查询最近的考试成绩
     */
    List<Score> selectRecentByStudentId(@Param("studentId") Integer studentId, 
                                         @Param("limit") Integer limit);
    
    /**
     * 更新考试状态
     */
    int updateStatus(@Param("id") Integer id, @Param("status") String status);
    
    /**
     * 更新总分
     */
    int updateTotalScore(@Param("id") Integer id, @Param("totalScore") Integer totalScore);
    
    /**
     * 检查学生是否已参加过某试卷
     */
    int countByStudentAndPaper(@Param("studentId") Integer studentId, 
                                @Param("paperId") Integer paperId);

    List<ScoreListVO> selectByTeacherId(Integer teacherId);
    
    /**
     * 根据教师ID和课程ID查询答卷列表
     */
    List<ScoreListVO> selectByTeacherIdAndCourseId(@Param("teacherId") Integer teacherId, 
                                                    @Param("courseId") Integer courseId);
    
    /**
     * 统计某教师某课程的答卷数量
     */
    int countByTeacherAndCourse(@Param("teacherId") Integer teacherId, 
                                @Param("courseId") Integer courseId);
    
    /**
     * 根据试卷ID查询考试记录
     */
    List<Score> selectByPaperId(@Param("paperId") Integer paperId);
}