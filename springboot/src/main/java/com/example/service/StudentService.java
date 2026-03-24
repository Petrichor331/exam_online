package com.example.service;


import com.example.common.Constants;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Student;
import com.example.exception.CustomException;
import com.example.mapper.StudentMapper;
import com.example.utils.BCryptUtils;
import com.example.utils.PasswordValidator;
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
    @Resource
    private UserDisableService userDisableService;
    //对应后台管理那个页面里面的新增学生，跟注册不一样
    public void add(Student student){
        Student dbStudent = studentMapper.selectByUsername(student.getUsername());
        if(dbStudent!=null){
            throw new CustomException(ResultCodeEnum.USER_EXIST_ERROR);
        }
        if(ObjectUtils.isEmpty(student.getPassword())){
            student.setPassword(Constants.USER_DEFALUT_PASSWORD);
        }
        if (!PasswordValidator.isValid(student.getPassword())) {
            throw new CustomException(PasswordValidator.getErrorMessage());
        }
        student.setPassword(BCryptUtils.encode(student.getPassword()));
        if(ObjectUtils.isEmpty(student.getName())){
            student.setName(student.getUsername());
        }
        student.setRole(RoleEnum.STUDENT.name());
        student.setStatus("正常");
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
        boolean matches = false;
        String dbPassword = dbStudent.getPassword();
        // 尝试BCrypt匹配
        if (dbPassword != null && dbPassword.startsWith("$2")) {
            matches = BCryptUtils.matches(account.getPassword(), dbPassword);
        }
        // 如果BCrypt匹配失败，尝试明文匹配（兼容旧数据）
        if (!matches) {
            matches = account.getPassword().equals(dbPassword);
            // 如果明文匹配成功，自动升级为BCrypt
            if (matches && dbPassword != null && !dbPassword.startsWith("$2")) {
                dbStudent.setPassword(BCryptUtils.encode(dbStudent.getPassword()));
                studentMapper.updateById(dbStudent);
            }
        }
        if (!matches) {
            throw new CustomException(ResultCodeEnum.USER_ACCOUNT_OR_PASSWORD_ERROR);
        }
        if(!dbStudent.getStatus().equals("正常")){
            throw new CustomException("账号状态异常，请联系管理员");
        }
        
        // 检查Redis是否被禁用/禁言
        String disableInfo = userDisableService.checkUserStatus("student", dbStudent.getId());
        if (disableInfo != null) {
            throw new CustomException(disableInfo);
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
        if(!BCryptUtils.matches(account.getPassword(), dbStudent.getPassword())){
            throw new CustomException(ResultCodeEnum.USER_ACCOUNT_OR_PASSWORD_ERROR);
        }
        dbStudent.setPassword(BCryptUtils.encode(account.getNewPassword()));
        studentMapper.updateById(dbStudent);
    }

    public Integer register(Account account) {
        Student student = new Student();
        BeanUtils.copyProperties(account,student);
        add(student);
        return student.getId();
    }

    /**
     * 根据邮箱查询学生
     */
    public Student selectByEmail(String email) {
        return studentMapper.selectByEmail(email);
    }

    /**
     * 根据用户名查询学生
     */
    public Student selectByUsername(String username) {
        return studentMapper.selectByUsername(username);
    }

    /**
     * 更新用户名和密码
     */
    public void updateUsernameAndPassword(Integer id, String username, String password) {
        Student student = studentMapper.selectById(id);
        if(student == null){
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        student.setUsername(username);
        student.setPassword(BCryptUtils.encode(password));
        studentMapper.updateById(student);
    }

    /**
     * 更新用户信息
     */
    public void updateUserInfo(Integer id, String name, String phone, String avatar) {
        Student student = studentMapper.selectById(id);
        if(student == null){
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if(name != null && !name.isEmpty()){
            student.setName(name);
        }
        if(phone != null && !phone.isEmpty()){
            student.setPhone(phone);
        }
        if(avatar != null && !avatar.isEmpty()){
            student.setAvatar(avatar);
        }
        studentMapper.updateById(student);
    }
}
