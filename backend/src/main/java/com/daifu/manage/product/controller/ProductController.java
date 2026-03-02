package com.daifu.manage.product.controller;

import com.daifu.manage.common.api.PageResult;
import com.daifu.manage.common.api.Result;
import com.daifu.manage.product.dto.ProductCreateRequest;
import com.daifu.manage.product.dto.ProductUpdateRequest;
import com.daifu.manage.product.dto.ProductUpdateStatusRequest;
import com.daifu.manage.product.service.ProductService;
import com.daifu.manage.product.vo.ProductVO;
import jakarta.validation.Valid;
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
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public Result<ProductVO> create(@Valid @RequestBody ProductCreateRequest request) {
        return Result.ok(productService.create(request));
    }

    @PutMapping("/{id}")
    public Result<ProductVO> update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        return Result.ok(productService.update(id, request));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody ProductUpdateStatusRequest request) {
        productService.updateStatus(id, request.status());
        return Result.ok();
    }

    @GetMapping
    public Result<PageResult<ProductVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                              @RequestParam(required = false) Long storeId,
                                              @RequestParam(required = false) String name,
                                              @RequestParam(required = false) Integer status) {
        return Result.ok(productService.page(pageNum, pageSize, storeId, name, status));
    }

    @GetMapping("/all")
    public Result<List<ProductVO>> allEnabled(@RequestParam(required = false) Long storeId) {
        return Result.ok(productService.allEnabled(storeId));
    }
}
