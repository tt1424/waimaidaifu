package com.daifu.manage.user.controller;

import com.daifu.manage.common.api.PageResult;
import com.daifu.manage.common.api.Result;
import com.daifu.manage.user.dto.UserCreateRequest;
import com.daifu.manage.user.dto.UserUpdateRequest;
import com.daifu.manage.user.service.UserService;
import com.daifu.manage.user.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public Result<UserVO> create(@Valid @RequestBody UserCreateRequest request) {
        return Result.ok(userService.create(request));
    }

    @PutMapping("/{id}")
    public Result<UserVO> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return Result.ok(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    @GetMapping
    public Result<PageResult<UserVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "10") Integer pageSize,
                                           @RequestParam(required = false) String username,
                                           @RequestParam(required = false) String phone,
                                           @RequestParam(required = false) Integer role,
                                           @RequestParam(required = false) Integer status) {
        return Result.ok(userService.page(pageNum, pageSize, username, phone, role, status));
    }
}
