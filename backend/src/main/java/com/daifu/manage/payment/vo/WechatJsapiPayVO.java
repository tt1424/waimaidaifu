package com.daifu.manage.payment.vo;

public record WechatJsapiPayVO(
        String orderNo,
        String appId,
        String timeStamp,
        String nonceStr,
        String packageValue,
        String signType,
        String paySign,
        String prepayId
) {
}
