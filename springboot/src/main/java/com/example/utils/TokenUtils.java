package com.example.utils;


import cn.hutool.core.date.DateUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.common.Constants;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.exception.CustomException;
import com.example.service.AdminService;
import com.example.service.StudentService;
import com.example.service.TeacherService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.PrivilegedAction;
import java.util.Date;


@Component
public class TokenUtils {
    private static final Logger logger = LoggerFactory.getLogger(TokenUtils.class);
    @Resource
    private AdminService adminService;
    @Resource
    private TeacherService teacherService;
    @Resource
    private StudentService studentService;

    private static AdminService staticAdminService;
    private static TeacherService staticTeacherService;
    private static StudentService staticStudentService;
    @PostConstruct
    public void init(){
        //在启动的时候就初始化，方便后面直接使用
        staticAdminService = adminService;
        staticTeacherService = teacherService;
        staticStudentService = studentService;
    }
    /**
     * 生成token
     */
    public static String createToken(String data, String sign){
        //audience是存储数据的一个媒介，存储用户ID和用户角色（1-ADMIN）然后就可以查数据了；sign是用户相对唯一的东西
        return JWT.create().withAudience(data)
                .withExpiresAt(DateUtil.offsetDay(new Date(), 1))//1天后过期
                .sign(Algorithm.HMAC256(sign)); //把用户密码加密后再进行签名

        //最后得到的是一个类似xxxx.yyyy.zzzz的字符串 Header.Payload.Signature
    }
    /**
     * 获取当前登录的用户信息
     */
    public static Account getCurrentUser(){
        try{
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            String token = request.getHeader(Constants.TOKEN);
            String audience = JWT.decode(token).getAudience().get(0);
            Integer userId = Integer.valueOf(audience.split("-")[0]);
            String role = audience.split("-")[1];
            if(RoleEnum.ADMIN.name().equals( role)){
                return staticAdminService.selectById(userId);
            }
            if(RoleEnum.TEACHER.name().equals( role)){
                return staticTeacherService.selectById(userId);
            }
            if (RoleEnum.STUDENT.name().equals( role)){
                return staticStudentService.selectById(userId);
            }

        }catch (Exception e){
            logger.error("获取当前登录用户出错",e);
        }
        return null;
    }
}
