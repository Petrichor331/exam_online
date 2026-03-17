package com.example.utils;

import java.util.regex.Pattern;

public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");

    public static boolean isValid(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return false;
        }
        boolean hasUpper = UPPERCASE.matcher(password).find();
        boolean hasLower = LOWERCASE.matcher(password).find();
        boolean hasDigit = DIGIT.matcher(password).find();
        return hasUpper && hasLower && hasDigit;
    }

    public static String getErrorMessage() {
        return "密码必须包含大小写字母和数字，且至少8位";
    }
}
