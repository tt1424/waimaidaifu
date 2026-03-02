package com.daifu.manage.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.daifu.manage.auth.dto.LoginRequest;
import com.daifu.manage.auth.dto.LoginResponse;
import com.daifu.manage.common.exception.BizException;
import com.daifu.manage.user.entity.SysUser;
import com.daifu.manage.user.mapper.SysUserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final SysUserMapper sysUserMapper;

    public AuthService(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.username())
                .last("limit 1"));

        if (user == null) {
            throw new BizException("account not found");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException("account disabled");
        }
        if (!ENCODER.matches(request.password(), user.getPassword())) {
            throw new BizException("invalid password");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        return new LoginResponse(token, 7200L, user.getId(), user.getUsername(), user.getRole());
    }
}
