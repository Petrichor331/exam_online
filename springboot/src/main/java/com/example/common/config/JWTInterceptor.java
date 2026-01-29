package com.example.common.config;

import cn.hutool.core.util.ObjectUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.example.common.Constants;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.Account;
import com.example.exception.CustomException;
import com.example.service.AdminService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT拦截器
 */
@Component
public class JWTInterceptor implements HandlerInterceptor {

    @Resource
    private AdminService adminService;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //从http请求头里取token
        String token = request.getHeader(Constants.TOKEN);
        if(ObjectUtil.isNull( token)){
            //如果没拿到，再从请求参数中拿一次
            request.getParameter(Constants.TOKEN);
        }
        //开始执行认证
        if (ObjectUtil.isNull( token)) {
            throw new CustomException(ResultCodeEnum.TOKEN_INVALID_ERROR);
        }
        Account account = null;

        try{
            //解析token
            String audience = JWT.decode(token).getAudience().get(0);
            String userId = audience.split("-")[0];
            String role = audience.split("-")[1];
            //根据用户角色判断用户属于哪个数据库表然后查询用户数据
            if(RoleEnum.ADMIN.name().equals( role)){
                account = adminService.selectById(Integer.valueOf(userId));
            }
        }catch (Exception e){
            throw new CustomException(ResultCodeEnum.TOKEN_CHECK_ERROR);
        }
        //根据token里携带的用户id去对应的用户表查询没查到，所以报用户不存在的错误。
        if(ObjectUtils.isEmpty(account)){
            throw new CustomException(ResultCodeEnum.USER_NOT_EXIST_ERROR);
        }
        try{
            //通过用户的密码作为密钥再次验证token的合法性
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(account.getPassword())).build();
            jwtVerifier.verify(token);
        }catch (JWTVerificationException e){
            throw new CustomException(ResultCodeEnum.TOKEN_CHECK_ERROR);
        }
        return true;
    }

}
