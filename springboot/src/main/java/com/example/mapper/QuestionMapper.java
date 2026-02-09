package com.example.mapper;

import com.example.common.vo.QuestionListVO;
import com.example.entity.Question;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface QuestionMapper {

    int insert(Question question);

    List<Question> selectAll(Question question);

    void updateById(Question question);

    void deleteById(Integer id);

    @Select("select * from `question` where id=#{id}")
    Question selectById(Integer id);

    List<QuestionListVO> selectPage(@Param("name") String name,@Param("courseId") Integer courseId,@Param("typeId") Integer typeId);

    @Select("select id from question where course_id=#{courseId}")
    List<Integer> selectIdsByCourseId(Integer courseId);

    @Select("select distinct knowledge_point from question where course_id=#{courseId}")
    List<String> selectKnowledgePoints(Integer courseId);
}
