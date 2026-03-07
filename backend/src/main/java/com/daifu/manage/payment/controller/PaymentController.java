package com.daifu.manage.payment.controller;

import com.daifu.manage.common.api.Result;
import com.daifu.manage.payment.dto.CheckoutCreateRequest;
import com.daifu.manage.payment.dto.WechatJsapiPayRequest;
import com.daifu.manage.payment.service.PaymentService;
import com.daifu.manage.payment.vo.CheckoutCreateVO;
import com.daifu.manage.payment.vo.PaymentOrderVO;
import com.daifu.manage.payment.vo.WechatJsapiPayVO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/pay")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/checkout")
    public Result<CheckoutCreateVO> createCheckout(@Valid @RequestBody CheckoutCreateRequest request) {
        return Result.ok(paymentService.createCheckout(request));
    }

    @PostMapping("/wechat/jsapi")
    public Result<WechatJsapiPayVO> jsapiPay(@Valid @RequestBody WechatJsapiPayRequest request) {
        return Result.ok(paymentService.wechatJsapiPay(request));
    }

    @GetMapping("/orders/{orderNo}")
    public Result<PaymentOrderVO> order(@PathVariable String orderNo) {
        return Result.ok(paymentService.getOrder(orderNo));
    }

    @PostMapping("/orders/{orderNo}/mock/success")
    public Result<Void> mockSuccess(@PathVariable String orderNo) {
        paymentService.simulatePaySuccess(orderNo);
        return Result.ok();
    }

    @PostMapping("/wechat/notify")
    public ResponseEntity<Map<String, String>> notify(@RequestBody String body,
                                                       @RequestHeader Map<String, String> headers) {
        boolean handled = paymentService.handleWechatNotify(body, headers);
        if (handled) {
            return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "success"));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("code", "FAIL", "message", "verify failed"));
    }
}
