package com.daifu.manage.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.daifu.manage.auth.dto.LoginRequest;
import com.daifu.manage.auth.dto.LoginResponse;
import com.daifu.manage.auth.entity.AdminUser;
import com.daifu.manage.auth.mapper.AdminUserMapper;
import com.daifu.manage.common.exception.BizException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final AdminUserMapper adminUserMapper;

    public AuthService(AdminUserMapper adminUserMapper) {
        this.adminUserMapper = adminUserMapper;
    }

    public LoginResponse login(LoginRequest request) {
        AdminUser adminUser = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, request.username())
                .last("limit 1"));

        if (adminUser == null) {
            throw new BizException("account not found");
        }
        if (adminUser.getStatus() == null || adminUser.getStatus() != 1) {
            throw new BizException("account disabled");
        }
        if (!ENCODER.matches(request.password(), adminUser.getPassword())) {
            throw new BizException("invalid password");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        return new LoginResponse(token, 7200L);
    }
}
