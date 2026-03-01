package com.daifu.manage;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TempPwd {
    public static void main(String[] args) {
        System.out.println(new BCryptPasswordEncoder().encode("123456"));
    }
}
