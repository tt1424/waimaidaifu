package com.daifu.manage.store.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.daifu.manage.common.api.PageResult;
import com.daifu.manage.common.exception.BizException;
import com.daifu.manage.store.dto.StoreCreateRequest;
import com.daifu.manage.store.dto.StoreUpdateRequest;
import com.daifu.manage.store.entity.Store;
import com.daifu.manage.store.mapper.StoreMapper;
import com.daifu.manage.store.vo.StoreVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class StoreService {

    private final StoreMapper storeMapper;

    public StoreService(StoreMapper storeMapper) {
        this.storeMapper = storeMapper;
    }

    public StoreVO create(StoreCreateRequest request) {
        Store store = new Store();
        store.setName(request.name());
        store.setContactName(request.contactName());
        store.setContactPhone(request.contactPhone());
        store.setStatus(request.status() == null ? 1 : request.status());
        storeMapper.insert(store);
        return toVO(store);
    }

    public StoreVO update(Long id, StoreUpdateRequest request) {
        Store store = storeMapper.selectById(id);
        if (store == null) {
            throw new BizException("store not found");
        }
        store.setName(request.name());
        store.setContactName(request.contactName());
        store.setContactPhone(request.contactPhone());
        store.setStatus(request.status());
        storeMapper.updateById(store);
        return toVO(store);
    }

    public void delete(Long id) {
        Store store = storeMapper.selectById(id);
        if (store == null) {
            throw new BizException("store not found");
        }
        storeMapper.deleteById(id);
    }

    public PageResult<StoreVO> page(Integer pageNum, Integer pageSize, String name, Integer status) {
        LambdaQueryWrapper<Store> wrapper = new LambdaQueryWrapper<Store>()
                .orderByDesc(Store::getId);
        if (StringUtils.hasText(name)) {
            wrapper.like(Store::getName, name);
        }
        if (status != null) {
            wrapper.eq(Store::getStatus, status);
        }
        IPage<Store> page = storeMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<StoreVO> records = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(page.getTotal(), pageNum, pageSize, records);
    }

    public List<StoreVO> allEnabled() {
        return storeMapper.selectList(new LambdaQueryWrapper<Store>()
                        .eq(Store::getStatus, 1)
                        .orderByDesc(Store::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    public boolean existsOpenStore(Long id) {
        return storeMapper.selectCount(new LambdaQueryWrapper<Store>()
                .eq(Store::getId, id)
                .eq(Store::getStatus, 1)) > 0;
    }

    private StoreVO toVO(Store store) {
        return new StoreVO(
                store.getId(),
                store.getName(),
                store.getContactName(),
                store.getContactPhone(),
                store.getStatus(),
                store.getCreateTime()
        );
    }
}
