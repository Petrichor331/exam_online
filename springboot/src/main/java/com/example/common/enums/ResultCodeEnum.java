package com.example.common.enums;

public enum  ResultCodeEnum {

    SUCCESS("成功","200"),
    PARAM_ERROR("400","参数异常"),
    TOKEN_INVALID_ERROR("401","无效的Token"),
    TOKEN_CHECK_ERROR("402","Token验证失败"),
    PARAM_LOST_ERROR("4001", "参数缺失"),
    SYSTEM_ERROR("500", "系统异常"),
    USER_EXIST_ERROR("5001", "用户名已存在"),
    USER_NOT_LOGIN("5002", "用户未登录"),
    USER_ACCOUNT_OR_PASSWORD_ERROR("5003", "账号或密码错误"),
    USER_NOT_EXIST_ERROR("5004", "用户不存在"),
    PARAM_PASSWORD_ERROR("5005", "原密码输入错误"),
    USER_NOT_PASS_ERROR("5006", "用户暂未通过审核,请耐心等待");

    private String code;
    private String msg;

    ResultCodeEnum(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
