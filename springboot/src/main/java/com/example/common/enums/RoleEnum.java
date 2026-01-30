package com.example.common.enums;

public enum RoleEnum {
    ADMIN("管理员"),
    TEACHER("教师"),
    STUDENT("学生");

    private String name;
    RoleEnum(String name) {
        this.name = name;
    }


}
