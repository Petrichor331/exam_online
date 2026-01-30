package com.example.service;


import com.example.common.Constants;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Student;
import com.example.exception.CustomException;
import com.example.mapper.StudentMapper;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;

@Service
public class StudentService {
    @Resource
    private StudentMapper studentMapper;
    //对应后台管理那个页面里面的新增学生，跟注册不一样
    public void add(Student student){
        Student dbStudent = studentMapper.selectByUsername(student.getUsername());
        if(dbStudent!=null){
            throw new CustomException(ResultCodeEnum.USER_EXIST_ERROR);
        }
        if(ObjectUtils.isEmpty(student.getPassword())){
            student.setPassword(Constants.USER_DEFALUT_PASSWORD);
        }
        if(ObjectUtils.isEmpty(student.getName())){
            student.setName(student.getUsername());
        }
        student.setRole(RoleEnum.STUDENT.name());
        student.setStatus("待审核");
        studentMapper.insert(student);
    }

    public PageInfo<Student> selectPage(Student student,Integer pageNum, Integer pageSize){
        PageHelper.startPage(pageNum,pageSize);
        List<Student> list = studentMapper.selectAll(student);
        return PageInfo.of(list);
    }
    public List<Student> selectAll(Student student) {
        return studentMapper.selectAll(student);
    }
    public Student selectById(Integer id) {
        return studentMapper.selectById(id);
    }

    public void updateById(Student student) {
        studentMapper.updateById(student);
    }

    public void deleteById(Integer id) {
        studentMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for(Integer id:ids){
            studentMapper.deleteById(id);
        }
    }

    public Student login(Account account) {
        Student dbStudent = studentMapper.selectByUsername(account.getUsername());
        if(dbStudent==null){
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if(!dbStudent.getPassword().equals(account.getPassword())){
            throw new CustomException(ResultCodeEnum.USER_ACCOUNT_OR_PASSWORD_ERROR);
        }
        if(!dbStudent.getStatus().equals("审核通过")){
            throw new CustomException(ResultCodeEnum.USER_NOT_PASS_ERROR);
        }

        String token = TokenUtils.createToken(dbStudent.getId()+"-"+dbStudent.getRole(), dbStudent.getPassword());
        dbStudent.setToken(token);
        return dbStudent;

    }

    public void updatePassword(Account account) {
        Student dbStudent = studentMapper.selectByUsername(account.getUsername());
        if(ObjectUtils.isEmpty(dbStudent)){
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if(!dbStudent.getPassword().equals(account.getPassword())){
            throw new CustomException(ResultCodeEnum.USER_ACCOUNT_OR_PASSWORD_ERROR);
        }
        dbStudent.setPassword(account.getNewPassword());
        studentMapper.updateById(dbStudent);
    }

    public void register(Account account) {
        Student student = new Student();
        BeanUtils.copyProperties(account,student);
        add( student);
    }
}
