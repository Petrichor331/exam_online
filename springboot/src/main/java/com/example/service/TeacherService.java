package com.example.service;


import com.example.common.Constants;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Teacher;
import com.example.exception.CustomException;
import com.example.mapper.TeacherMapper;
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
public class TeacherService {
    @Resource
    private TeacherMapper teacherMapper;
    @Resource
    private UserDisableService userDisableService;

    public void add(Teacher teacher){
        Teacher dbTeacher = teacherMapper.selectByUsername(teacher.getUsername());
        if(dbTeacher!=null){
            throw new CustomException(ResultCodeEnum.USER_EXIST_ERROR);
        }
        if(ObjectUtils.isEmpty(teacher.getPassword())){
            teacher.setPassword(Constants.USER_DEFALUT_PASSWORD);
        }
        if (!PasswordValidator.isValid(teacher.getPassword())) {
            throw new CustomException(PasswordValidator.getErrorMessage());
        }
        teacher.setPassword(BCryptUtils.encode(teacher.getPassword()));
        if(ObjectUtils.isEmpty(teacher.getName())){
            teacher.setName(teacher.getUsername());
        }
        teacher.setRole(RoleEnum.TEACHER.name());
        teacher.setStatus("正常");
        teacherMapper.insert(teacher);
    }

    public PageInfo<Teacher> selectPage(Teacher teacher,Integer pageNum, Integer pageSize){
        PageHelper.startPage(pageNum,pageSize);
        List<Teacher> list = teacherMapper.selectAll(teacher);
        return PageInfo.of(list);
    }
    public List<Teacher> selectAll(Teacher teacher) {
        return teacherMapper.selectAll(teacher);
    }
    public Teacher selectById(Integer id) {
        return teacherMapper.selectById(id);
    }

    public void updateById(Teacher teacher) {
        teacherMapper.updateById(teacher);
    }

    public void deleteById(Integer id) {
        teacherMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for(Integer id:ids){
            teacherMapper.deleteById(id);
        }
    }

    public Teacher login(Account account) {
        Teacher dbTeacher = teacherMapper.selectByUsername(account.getUsername());
        if(dbTeacher==null){
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        boolean matches = false;
        String dbPassword = dbTeacher.getPassword();
        // 尝试BCrypt匹配
        if (dbPassword != null && dbPassword.startsWith("$2")) {
            matches = BCryptUtils.matches(account.getPassword(), dbPassword);
        }
        // 如果BCrypt匹配失败，尝试明文匹配（兼容旧数据）
        if (!matches) {
            matches = account.getPassword().equals(dbPassword);
            // 如果明文匹配成功，自动升级为BCrypt
            if (matches && dbPassword != null && !dbPassword.startsWith("$2")) {
                dbTeacher.setPassword(BCryptUtils.encode(dbTeacher.getPassword()));
                teacherMapper.updateById(dbTeacher);
            }
        }
        if (!matches) {
            throw new CustomException(ResultCodeEnum.USER_ACCOUNT_OR_PASSWORD_ERROR);
        }
        if(dbTeacher.getStatus() != null && !dbTeacher.getStatus().equals("正常")){
            throw new CustomException("账号状态异常，请联系管理员");
        }
        
        // 检查Redis是否被禁用/禁言
        String disableInfo = userDisableService.checkUserStatus("teacher", dbTeacher.getId());
        if (disableInfo != null) {
            throw new CustomException(disableInfo);
        }
        
        String token = TokenUtils.createToken(dbTeacher.getId()+"-"+dbTeacher.getRole(), dbTeacher.getPassword());
        dbTeacher.setToken(token);
        return dbTeacher;

    }

    public void updatePassword(Account account) {
        Teacher dbTeacher = teacherMapper.selectByUsername(account.getUsername());
        if(ObjectUtils.isEmpty(dbTeacher)){
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if(!BCryptUtils.matches(account.getPassword(), dbTeacher.getPassword())){
            throw new CustomException(ResultCodeEnum.USER_ACCOUNT_OR_PASSWORD_ERROR);
        }
        dbTeacher.setPassword(BCryptUtils.encode(account.getNewPassword()));
        teacherMapper.updateById(dbTeacher);
    }

    public Integer register(Account account) {
        Teacher teacher = new Teacher();
        BeanUtils.copyProperties(account,teacher);
        add(teacher);
        return teacher.getId();
    }

    /**
     * 根据邮箱查询教师
     */
    public Teacher selectByEmail(String email) {
        return teacherMapper.selectByEmail(email);
    }

    /**
     * 根据用户名查询教师
     */
    public Teacher selectByUsername(String username) {
        return teacherMapper.selectByUsername(username);
    }

    /**
     * 更新用户名和密码
     */
    public void updateUsernameAndPassword(Integer id, String username, String password) {
        Teacher teacher = teacherMapper.selectById(id);
        if(teacher == null){
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        teacher.setUsername(username);
        teacher.setPassword(BCryptUtils.encode(password));
        teacherMapper.updateById(teacher);
    }

    /**
     * 更新用户信息
     */
    public void updateUserInfo(Integer id, String name, String phone, String avatar) {
        Teacher teacher = teacherMapper.selectById(id);
        if(teacher == null){
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if(name != null && !name.isEmpty()){
            teacher.setName(name);
        }
        if(phone != null && !phone.isEmpty()){
            teacher.setPhone(phone);
        }
        if(avatar != null && !avatar.isEmpty()){
            teacher.setAvatar(avatar);
        }
        teacherMapper.updateById(teacher);
    }
}
