package com.daifu.manage.report.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.daifu.manage.order.entity.PayOrder;
import com.daifu.manage.order.mapper.PayOrderMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final PayOrderMapper payOrderMapper;

    public ReportService(PayOrderMapper payOrderMapper) {
        this.payOrderMapper = payOrderMapper;
    }

    public Map<String, Object> overview() {
        List<PayOrder> paidOrders = payOrderMapper.selectList(new LambdaQueryWrapper<PayOrder>()
                .eq(PayOrder::getStatus, 1));
        BigDecimal totalPayAmount = paidOrders.stream()
                .map(PayOrder::getPayAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<PayOrder> todayPaidOrders = payOrderMapper.selectList(new LambdaQueryWrapper<PayOrder>()
                .eq(PayOrder::getStatus, 1)
                .ge(PayOrder::getPayTime, start)
                .lt(PayOrder::getPayTime, end));

        BigDecimal todayPayAmount = todayPaidOrders.stream()
                .map(PayOrder::getPayAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long unpaidCount = payOrderMapper.selectCount(new LambdaQueryWrapper<PayOrder>()
                .eq(PayOrder::getStatus, 0));

        Map<String, Object> result = new HashMap<>();
        result.put("totalPayAmount", totalPayAmount);
        result.put("todayPayAmount", todayPayAmount);
        result.put("payCount", (long) paidOrders.size());
        result.put("unpaidCount", unpaidCount);
        return result;
    }
}
