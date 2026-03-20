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

    List<QuestionListVO> selectPage(@Param("name") String name,
                                    @Param("courseId") Integer courseId,
                                    @Param("typeId") Integer typeId,
                                    @Param("difficulty") Integer difficulty,
                                    @Param("knowledgePoints") List<String> knowledgePoints);

    @Select("select id from question where course_id=#{courseId}")
    List<Integer> selectIdsByCourseId(Integer courseId);

    @Select("select distinct knowledge_point from question where course_id=#{courseId}")
    List<String> selectKnowledgePoints(Integer courseId);

    @Select("select * from question where course_id=#{courseId}")
    List<Question> selectByCourseId(Integer courseId);

    List<Question> selectRandomQuestions(@Param("courseId") Integer courseId,
                                         @Param("typeIds") List<Integer> typeIds,
                                         @Param("knowledgePoints") List<String> knowledgePoints,
                                         @Param("difficulty") Integer difficulty,
                                         @Param("count") Integer count);

    List<Question> selectByCourseIdWithFilter(@Param("courseId") Integer courseId,
                                               @Param("typeIds") List<Integer> typeIds,
                                               @Param("knowledgePoints") List<String> knowledgePoints,
                                               @Param("difficulty") Integer difficulty);

    @Select("<script>SELECT * FROM question WHERE id IN " +
            "<foreach collection='list' item='id' open='(' separator=',' close=')'>" +
            "#{id}</foreach></script>")
    List<Question> selectByIds(@Param("list") List<Integer> questionIds);
}
