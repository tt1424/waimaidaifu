package com.daifu.manage.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.daifu.manage.common.api.PageResult;
import com.daifu.manage.common.exception.BizException;
import com.daifu.manage.user.dto.UserCreateRequest;
import com.daifu.manage.user.dto.UserUpdateRequest;
import com.daifu.manage.user.entity.SysUser;
import com.daifu.manage.user.mapper.SysUserMapper;
import com.daifu.manage.user.vo.UserVO;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UserService {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final SysUserMapper sysUserMapper;

    public UserService(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    public UserVO create(UserCreateRequest request) {
        boolean exists = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.username())) > 0;
        if (exists) {
            throw new BizException("username already exists");
        }
        SysUser user = new SysUser();
        user.setUsername(request.username());
        user.setPassword(ENCODER.encode(request.password()));
        user.setPhone(request.phone());
        user.setRole(request.role());
        user.setStatus(request.status() == null ? 1 : request.status());
        sysUserMapper.insert(user);
        return toVO(user);
    }

    public UserVO update(Long id, UserUpdateRequest request) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BizException("user not found");
        }
        boolean exists = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.username())
                .ne(SysUser::getId, id)) > 0;
        if (exists) {
            throw new BizException("username already exists");
        }
        user.setUsername(request.username());
        user.setPhone(request.phone());
        user.setRole(request.role());
        user.setStatus(request.status());
        if (StringUtils.hasText(request.password())) {
            user.setPassword(ENCODER.encode(request.password()));
        }
        sysUserMapper.updateById(user);
        return toVO(user);
    }

    public void delete(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BizException("user not found");
        }
        sysUserMapper.deleteById(id);
    }

    public PageResult<UserVO> page(Integer pageNum, Integer pageSize, String username, String phone, Integer role, Integer status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .orderByDesc(SysUser::getId);
        if (StringUtils.hasText(username)) {
            wrapper.like(SysUser::getUsername, username);
        }
        if (StringUtils.hasText(phone)) {
            wrapper.like(SysUser::getPhone, phone);
        }
        if (role != null) {
            wrapper.eq(SysUser::getRole, role);
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        IPage<SysUser> page = sysUserMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<UserVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(page.getTotal(), pageNum, pageSize, records);
    }

    public boolean existsEnabledUser(Long id) {
        return sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .eq(SysUser::getStatus, 1)) > 0;
    }

    private UserVO toVO(SysUser user) {
        return new UserVO(
                user.getId(),
                user.getUsername(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                user.getCreateTime()
        );
    }
}
