package com.example.service;


import com.example.common.Constants;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Admin;
import com.example.exception.CustomException;
import com.example.mapper.AdminMapper;
import com.example.utils.BCryptUtils;
import com.example.utils.PasswordValidator;
import com.example.utils.TokenUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;

@Service
public class AdminService {
    @Resource
    private AdminMapper adminMapper;

    public void add(Admin admin){
        Admin dbAdmin = adminMapper.selectByUsername(admin.getUsername());
        if(dbAdmin!=null){
            throw new CustomException(ResultCodeEnum.USER_EXIST_ERROR);
        }
        if(ObjectUtils.isEmpty(admin.getPassword())){
            admin.setPassword(Constants.USER_DEFALUT_PASSWORD);
        }
        if (!PasswordValidator.isValid(admin.getPassword())) {
            throw new CustomException(PasswordValidator.getErrorMessage());
        }
        admin.setPassword(BCryptUtils.encode(admin.getPassword()));
        if(ObjectUtils.isEmpty(admin.getName())){
            admin.setName(admin.getUsername());
        }
        admin.setRole(RoleEnum.ADMIN.name());
        adminMapper.insert(admin);
    }

    public PageInfo<Admin> selectPage(Admin admin,Integer pageNum, Integer pageSize){
        PageHelper.startPage(pageNum,pageSize);
        List<Admin> list = adminMapper.selectAll(admin);
        return PageInfo.of(list);
    }
    public List<Admin> selectAll(Admin admin) {
        return adminMapper.selectAll(admin);
    }
    public Admin selectById(Integer id) {
        return adminMapper.selectById(id);
    }

    public void updateById(Admin admin) {
        adminMapper.updateById(admin);
    }

    public void deleteById(Integer id) {
        adminMapper.deleteById(id);
    }

    public void deleteBatch(List<Integer> ids) {
        for(Integer id:ids){
            adminMapper.deleteById(id);
        }
    }

    public Admin login(Account account) {
        Admin dbAdmin = adminMapper.selectByUsername(account.getUsername());
        if(dbAdmin==null){
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        boolean matches = false;
        String dbPassword = dbAdmin.getPassword();
        // 尝试BCrypt匹配
        if (dbPassword != null && dbPassword.startsWith("$2")) {
            matches = BCryptUtils.matches(account.getPassword(), dbPassword);
        }
        // 如果BCrypt匹配失败，尝试明文匹配（兼容旧数据）
        if (!matches) {
            matches = account.getPassword().equals(dbPassword);
            // 如果明文匹配成功，自动升级为BCrypt
            if (matches && dbPassword != null && !dbPassword.startsWith("$2")) {
                dbAdmin.setPassword(BCryptUtils.encode(dbAdmin.getPassword()));
                adminMapper.updateById(dbAdmin);
            }
        }
        if (!matches) {
            throw new CustomException(ResultCodeEnum.USER_ACCOUNT_OR_PASSWORD_ERROR);
        }
        String token = TokenUtils.createToken(dbAdmin.getId()+"-"+dbAdmin.getRole(), dbAdmin.getPassword());
        dbAdmin.setToken(token);
        return dbAdmin;

    }

    public void updatePassword(Account account) {
        Admin dbAdmin = adminMapper.selectByUsername(account.getUsername());
        if(ObjectUtils.isEmpty(dbAdmin)){
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if(!BCryptUtils.matches(account.getPassword(), dbAdmin.getPassword())){
            throw new CustomException(ResultCodeEnum.USER_ACCOUNT_OR_PASSWORD_ERROR);
        }
        dbAdmin.setPassword(BCryptUtils.encode(account.getNewPassword()));
        adminMapper.updateById(dbAdmin);
    }
}
