package com.daifu.manage.store.controller;

import com.daifu.manage.common.api.PageResult;
import com.daifu.manage.common.api.Result;
import com.daifu.manage.store.dto.StoreCreateRequest;
import com.daifu.manage.store.dto.StoreUpdateRequest;
import com.daifu.manage.store.service.StoreService;
import com.daifu.manage.store.vo.StoreVO;
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

import java.util.List;

@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @PostMapping
    public Result<StoreVO> create(@Valid @RequestBody StoreCreateRequest request) {
        return Result.ok(storeService.create(request));
    }

    @PutMapping("/{id}")
    public Result<StoreVO> update(@PathVariable Long id, @Valid @RequestBody StoreUpdateRequest request) {
        return Result.ok(storeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        storeService.delete(id);
        return Result.ok();
    }

    @GetMapping
    public Result<PageResult<StoreVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "10") Integer pageSize,
                                            @RequestParam(required = false) String name,
                                            @RequestParam(required = false) Integer status) {
        return Result.ok(storeService.page(pageNum, pageSize, name, status));
    }

    @GetMapping("/all")
    public Result<List<StoreVO>> allEnabled() {
        return Result.ok(storeService.allEnabled());
    }
}
