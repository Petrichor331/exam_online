package com.example.service;


import com.example.common.Constants;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.entity.Admin;
import com.example.exception.CustomException;
import com.example.mapper.AdminMapper;
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
        if(!dbAdmin.getPassword().equals(account.getPassword())){
            throw new CustomException(ResultCodeEnum.USER_ACCOUNT_OR_PASSWORD_ERROR);
        }
        //TODO token
        String token = TokenUtils.createToken(dbAdmin.getId()+"-"+dbAdmin.getRole(), dbAdmin.getPassword());
        dbAdmin.setToken(token);
        return dbAdmin;

    }

    public void updatePassword(Account account) {
        Admin dbAdmin = adminMapper.selectByUsername(account.getUsername());
        if(ObjectUtils.isEmpty(dbAdmin)){
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        if(!dbAdmin.getPassword().equals(account.getPassword())){
            throw new CustomException(ResultCodeEnum.USER_ACCOUNT_OR_PASSWORD_ERROR);
        }
        dbAdmin.setPassword(account.getNewPassword());
        adminMapper.updateById(dbAdmin);
    }
}
