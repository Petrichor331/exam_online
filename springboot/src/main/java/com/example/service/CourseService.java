package com.example.service;


import cn.hutool.core.date.DateUtil;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Course;
import com.example.mapper.CourseMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {
    @Resource
    private CourseMapper courseMapper;

    public void add(Course course) {
        Account account = TokenUtils.getCurrentUser();
        course.setTeacherId(account.getId());
        courseMapper.insert(course);
    }
    public PageInfo<Course> selectPage(Course course, Integer pageNum, Integer pageSize) {
        Account account = TokenUtils.getCurrentUser();
        //如果是老师的话，设一下id，则会执行mapper中<if test="teacherId != null"> and teacher_id = #{teacherId}</if>
        if(RoleEnum.TEACHER.name().equals(account.getRole())){
            course.setTeacherId(account.getId());
        }
        PageHelper.startPage(pageNum, pageSize);
        List<Course> list = courseMapper.selectAll(course);
        return PageInfo.of(list);
    }

    public List<Course> selectAll(Course course) {
        return courseMapper.selectAll(course);
    }

    public Course selectById(Integer id) {
        return courseMapper.selectById(id);
    }

    public void updateById(Course course) {
        courseMapper.updateById(course);
    }

    public void deleteById(Integer id) {
        courseMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for (Integer id : ids) {
            courseMapper.deleteById(id);
        }
    }

}
