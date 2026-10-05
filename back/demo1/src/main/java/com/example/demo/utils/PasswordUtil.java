package com.example.demo.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    // 注册时使用：把用户输入的明文密码加密成 BCrypt 密文后再存数据库
    public static String encode(String rawPassword) {

        return ENCODER.encode(rawPassword);
    }

    // 登录时使用：用用户输入的明文密码，匹配数据库里的 BCrypt 密文
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (encodedPassword == null || encodedPassword.trim().isEmpty()) {
            return false;
        }

        return ENCODER.matches(rawPassword, encodedPassword);
    }

    // BCrypt 密文一般以 $2a$、$2b$、$2y$ 开头，用它判断旧账号密码是否还没有加密
    public static boolean isEncoded(String password) {
        if (password == null) {
            return false;
        }

        return password.startsWith("$2a$")
                || password.startsWith("$2b$")
                || password.startsWith("$2y$");
    }
}
