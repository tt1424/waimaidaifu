package com.daifu.manage.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.daifu.manage.common.exception.BizException;
import com.daifu.manage.product.dto.ProductCreateRequest;
import com.daifu.manage.product.dto.ProductUpdateRequest;
import com.daifu.manage.product.entity.Product;
import com.daifu.manage.product.mapper.ProductMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProductService {

    private final ProductMapper productMapper;

    public ProductService(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    public Product create(ProductCreateRequest request) {
        Product product = new Product();
        product.setName(request.name());
        product.setPrice(request.price());
        product.setStatus(1);
        productMapper.insert(product);
        return product;
    }

    public Product update(Long id, ProductUpdateRequest request) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException("product not found");
        }
        product.setName(request.name());
        product.setPrice(request.price());
        productMapper.updateById(product);
        return product;
    }

    public void updateStatus(Long id, Integer status) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException("product not found");
        }
        product.setStatus(status);
        productMapper.updateById(product);
    }

    public IPage<Product> list(Integer pageNum, Integer pageSize, String name, Integer status) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .orderByDesc(Product::getId);

        if (StringUtils.hasText(name)) {
            wrapper.like(Product::getName, name);
        }
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }

        return productMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }
}
