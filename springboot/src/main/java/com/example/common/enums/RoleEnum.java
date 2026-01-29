package com.example.common.enums;

public enum RoleEnum {
    ADMIN("管理员"),
    USER("用户");

    private String name;
    RoleEnum(String name) {
        this.name = name;
    }

}
