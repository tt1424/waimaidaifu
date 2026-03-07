package com.daifu.manage.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.wechat")
public class WechatPayProperties {

    private String mode = "mock";
    private String appId;
    private String mchId;
    private String mchSerialNo;
    private String privateKeyPem;
    private String platformPublicKeyPem;
    private String apiV3Key;
    private String notifyUrl;
    private String gateway = "https://api.mch.weixin.qq.com";
    private Integer orderExpireMinutes = 30;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getMchId() {
        return mchId;
    }

    public void setMchId(String mchId) {
        this.mchId = mchId;
    }

    public String getMchSerialNo() {
        return mchSerialNo;
    }

    public void setMchSerialNo(String mchSerialNo) {
        this.mchSerialNo = mchSerialNo;
    }

    public String getPrivateKeyPem() {
        return privateKeyPem;
    }

    public void setPrivateKeyPem(String privateKeyPem) {
        this.privateKeyPem = privateKeyPem;
    }

    public String getPlatformPublicKeyPem() {
        return platformPublicKeyPem;
    }

    public void setPlatformPublicKeyPem(String platformPublicKeyPem) {
        this.platformPublicKeyPem = platformPublicKeyPem;
    }

    public String getApiV3Key() {
        return apiV3Key;
    }

    public void setApiV3Key(String apiV3Key) {
        this.apiV3Key = apiV3Key;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public Integer getOrderExpireMinutes() {
        return orderExpireMinutes;
    }

    public void setOrderExpireMinutes(Integer orderExpireMinutes) {
        this.orderExpireMinutes = orderExpireMinutes;
    }
}
