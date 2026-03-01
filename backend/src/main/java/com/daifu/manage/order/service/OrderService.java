package com.daifu.manage.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.daifu.manage.common.exception.BizException;
import com.daifu.manage.order.dto.OrderCreateRequest;
import com.daifu.manage.order.dto.PayNotifyRequest;
import com.daifu.manage.order.entity.PayNotifyLog;
import com.daifu.manage.order.entity.PayOrder;
import com.daifu.manage.order.mapper.PayNotifyLogMapper;
import com.daifu.manage.order.mapper.PayOrderMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PayOrderMapper payOrderMapper;
    private final PayNotifyLogMapper payNotifyLogMapper;

    @Value("${app.pay.base-url:https://your-domain/pay}")
    private String payBaseUrl;

    public OrderService(PayOrderMapper payOrderMapper, PayNotifyLogMapper payNotifyLogMapper) {
        this.payOrderMapper = payOrderMapper;
        this.payNotifyLogMapper = payNotifyLogMapper;
    }

    public Map<String, Object> create(OrderCreateRequest request) {
        String orderNo = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(request.expireMinutes());
        String payUrl = payBaseUrl + "?orderNo=" + orderNo;

        PayOrder order = new PayOrder();
        order.setOrderNo(orderNo);
        order.setProductId(request.productId());
        order.setProductName(request.productName());
        order.setAmount(request.amount());
        order.setStatus(0);
        order.setPayUrl(payUrl);
        order.setExpireTime(expireTime);
        payOrderMapper.insert(order);

        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("productName", request.productName());
        result.put("amount", request.amount());
        result.put("payUrl", payUrl);
        result.put("expireTime", expireTime.format(FMT));
        return result;
    }

    public IPage<PayOrder> list(Integer pageNum, Integer pageSize, Integer status, String orderNo) {
        LambdaQueryWrapper<PayOrder> wrapper = new LambdaQueryWrapper<PayOrder>()
                .orderByDesc(PayOrder::getId);
        if (status != null) {
            wrapper.eq(PayOrder::getStatus, status);
        }
        if (orderNo != null && !orderNo.isEmpty()) {
            wrapper.like(PayOrder::getOrderNo, orderNo);
        }
        return payOrderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> payNotify(PayNotifyRequest request) {
        PayOrder order = payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrder>()
                .eq(PayOrder::getOrderNo, request.orderNo())
                .last("limit 1"));
        if (order == null) {
            throw new BizException("order not found");
        }

        PayNotifyLog log = new PayNotifyLog();
        log.setOrderNo(request.orderNo());
        log.setNotifyData("payAmount=" + request.payAmount() + ",payTime=" + request.payTime());

        if (order.getStatus() != null && order.getStatus() == 1) {
            log.setNotifyStatus(1);
            payNotifyLogMapper.insert(log);
            return Map.of("orderNo", request.orderNo(), "notify", "ignored_paid");
        }

        LocalDateTime payTime;
        try {
            payTime = LocalDateTime.parse(request.payTime(), FMT);
        } catch (DateTimeParseException ex) {
            throw new BizException("invalid payTime format, expected yyyy-MM-dd HH:mm:ss");
        }

        order.setStatus(1);
        order.setPayAmount(request.payAmount());
        order.setPayTime(payTime);
        payOrderMapper.updateById(order);

        log.setNotifyStatus(1);
        payNotifyLogMapper.insert(log);
        return Map.of("orderNo", request.orderNo(), "notify", "accepted");
    }
}

