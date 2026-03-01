package com.daifu.manage.product.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.daifu.manage.common.api.ApiResponse;
import com.daifu.manage.product.dto.ProductCreateRequest;
import com.daifu.manage.product.dto.ProductUpdateRequest;
import com.daifu.manage.product.dto.ProductUpdateStatusRequest;
import com.daifu.manage.product.entity.Product;
import com.daifu.manage.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/create")
    public ApiResponse<Product> create(@Valid @RequestBody ProductCreateRequest request) {
        return ApiResponse.ok(productService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Product> update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        return ApiResponse.ok(productService.update(id, request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody ProductUpdateStatusRequest request) {
        productService.updateStatus(id, request.status());
        return ApiResponse.ok();
    }

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                                  @RequestParam(defaultValue = "10") Integer pageSize,
                                                  @RequestParam(required = false) String name,
                                                  @RequestParam(required = false) Integer status) {
        IPage<Product> page = productService.list(pageNum, pageSize, name, status);
        return ApiResponse.ok(Map.of(
                "pageNum", pageNum,
                "pageSize", pageSize,
                "total", page.getTotal(),
                "records", page.getRecords()
        ));
    }
}
