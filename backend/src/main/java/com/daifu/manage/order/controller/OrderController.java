package com.daifu.manage.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.daifu.manage.common.api.ApiResponse;
import com.daifu.manage.order.dto.OrderCreateRequest;
import com.daifu.manage.order.dto.PayNotifyRequest;
import com.daifu.manage.order.entity.PayOrder;
import com.daifu.manage.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/create")
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody OrderCreateRequest request) {
        return ApiResponse.ok(orderService.create(request));
    }

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                                  @RequestParam(defaultValue = "10") Integer pageSize,
                                                  @RequestParam(required = false) Integer status,
                                                  @RequestParam(required = false) String orderNo) {
        IPage<PayOrder> page = orderService.list(pageNum, pageSize, status, orderNo);
        return ApiResponse.ok(Map.of(
                "pageNum", pageNum,
                "pageSize", pageSize,
                "total", page.getTotal(),
                "records", page.getRecords()
        ));
    }

    @PostMapping("/payNotify")
    public ApiResponse<Map<String, Object>> payNotify(@Valid @RequestBody PayNotifyRequest request) {
        return ApiResponse.ok(orderService.payNotify(request));
    }
}
