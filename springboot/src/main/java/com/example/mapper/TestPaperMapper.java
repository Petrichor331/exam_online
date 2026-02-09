package com.example.mapper;

import com.example.common.vo.TestPaperVO;
import com.example.entity.TestPaper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TestPaperMapper {

    int insert(TestPaper testPaper);

    List<TestPaperVO> selectAll(TestPaper testPaper);

    void updateById(TestPaper testPaper);

    void deleteById(Integer id);

    @Select("select * from `testPaper` where id=#{id}")
    TestPaper selectById(Integer id);
}
