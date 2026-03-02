package com.daifu.manage.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.daifu.manage.common.api.PageResult;
import com.daifu.manage.common.exception.BizException;
import com.daifu.manage.product.dto.ProductCreateRequest;
import com.daifu.manage.product.dto.ProductUpdateRequest;
import com.daifu.manage.product.entity.Product;
import com.daifu.manage.product.mapper.ProductMapper;
import com.daifu.manage.product.vo.ProductVO;
import com.daifu.manage.store.entity.Store;
import com.daifu.manage.store.mapper.StoreMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductMapper productMapper;
    private final StoreMapper storeMapper;

    public ProductService(ProductMapper productMapper, StoreMapper storeMapper) {
        this.productMapper = productMapper;
        this.storeMapper = storeMapper;
    }

    public ProductVO create(ProductCreateRequest request) {
        ensureStoreExists(request.storeId());
        Product product = new Product();
        product.setStoreId(request.storeId());
        product.setName(request.name());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setDescription(request.description());
        product.setStatus(1);
        productMapper.insert(product);
        return toVO(product, getStoreName(product.getStoreId()));
    }

    public ProductVO update(Long id, ProductUpdateRequest request) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException("product not found");
        }
        ensureStoreExists(request.storeId());
        product.setStoreId(request.storeId());
        product.setName(request.name());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setDescription(request.description());
        productMapper.updateById(product);
        return toVO(product, getStoreName(product.getStoreId()));
    }

    public void updateStatus(Long id, Integer status) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException("product not found");
        }
        product.setStatus(status);
        productMapper.updateById(product);
    }

    public PageResult<ProductVO> page(Integer pageNum, Integer pageSize, Long storeId, String name, Integer status) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .orderByDesc(Product::getId);

        if (storeId != null) {
            wrapper.eq(Product::getStoreId, storeId);
        }
        if (StringUtils.hasText(name)) {
            wrapper.like(Product::getName, name);
        }
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }

        IPage<Product> page = productMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Map<Long, String> storeNames = findStoreNames(page.getRecords().stream()
                .map(Product::getStoreId)
                .collect(Collectors.toSet()));

        List<ProductVO> records = page.getRecords().stream()
                .map(item -> toVO(item, storeNames.getOrDefault(item.getStoreId(), "")))
                .toList();
        return PageResult.of(page.getTotal(), pageNum, pageSize, records);
    }

    public List<ProductVO> allEnabled(Long storeId) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .orderByDesc(Product::getId);
        if (storeId != null) {
            wrapper.eq(Product::getStoreId, storeId);
        }
        List<Product> products = productMapper.selectList(wrapper);
        Map<Long, String> storeNames = findStoreNames(products.stream()
                .map(Product::getStoreId)
                .collect(Collectors.toSet()));
        return products.stream()
                .map(item -> toVO(item, storeNames.getOrDefault(item.getStoreId(), "")))
                .toList();
    }

    public Product getEnabledProductById(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            throw new BizException("product not available");
        }
        return product;
    }

    private void ensureStoreExists(Long storeId) {
        if (storeId == null || storeMapper.selectById(storeId) == null) {
            throw new BizException("store not found");
        }
    }

    private String getStoreName(Long storeId) {
        Store store = storeMapper.selectById(storeId);
        return store == null ? "" : store.getName();
    }

    private Map<Long, String> findStoreNames(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return storeMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Store::getId, Store::getName));
    }

    private ProductVO toVO(Product product, String storeName) {
        return new ProductVO(
                product.getId(),
                product.getStoreId(),
                storeName,
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getDescription(),
                product.getStatus(),
                product.getCreateTime()
        );
    }
}
