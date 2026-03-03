package com.daifu.manage.payment.service;

import com.daifu.manage.common.exception.BizException;
import com.daifu.manage.payment.config.WechatPayProperties;
import com.daifu.manage.payment.vo.WechatJsapiPayVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class WechatPayClient {

    private final WechatPayProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public WechatPayClient(WechatPayProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public String unifiedOrderJsapi(String orderNo, BigDecimal totalAmount, String openid) {
        ensureConfigForPay();
        try {
            int totalFen = totalAmount.multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValueExact();

            Map<String, Object> payload = new HashMap<>();
            payload.put("appid", properties.getAppId());
            payload.put("mchid", properties.getMchId());
            payload.put("description", "购物车结算-" + orderNo);
            payload.put("out_trade_no", orderNo);
            payload.put("notify_url", properties.getNotifyUrl());
            payload.put("amount", Map.of("total", totalFen, "currency", "CNY"));
            payload.put("payer", Map.of("openid", openid));

            String path = "/v3/pay/transactions/jsapi";
            String body = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimGateway() + path))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", buildAuthorization("POST", path, body))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException("wechat unified order failed: " + response.body());
            }
            JsonNode node = objectMapper.readTree(response.body());
            String prepayId = node.path("prepay_id").asText("");
            if (!StringUtils.hasText(prepayId)) {
                throw new BizException("wechat unified order missing prepay_id");
            }
            return prepayId;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException("wechat unified order error: " + ex.getMessage());
        }
    }

    public WechatJsapiPayVO buildJsapiPayParams(String orderNo, String prepayId) {
        ensureConfigForPay();
        try {
            String appId = properties.getAppId();
            String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
            String nonceStr = UUID.randomUUID().toString().replace("-", "");
            String packageValue = "prepay_id=" + prepayId;
            String signType = "RSA";
            String message = appId + "\n" + timeStamp + "\n" + nonceStr + "\n" + packageValue + "\n";
            String paySign = sign(message);

            return new WechatJsapiPayVO(
                    orderNo,
                    appId,
                    timeStamp,
                    nonceStr,
                    packageValue,
                    signType,
                    paySign,
                    prepayId
            );
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException("build jsapi pay params failed: " + ex.getMessage());
        }
    }

    public boolean verifySignature(String timestamp, String nonce, String body, String signatureText) {
        ensureConfigForNotify();
        if (!StringUtils.hasText(timestamp) || !StringUtils.hasText(nonce)
                || !StringUtils.hasText(body) || !StringUtils.hasText(signatureText)) {
            return false;
        }
        try {
            String message = timestamp + "\n" + nonce + "\n" + body + "\n";
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(loadPlatformPublicKey());
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signatureText));
        } catch (Exception ex) {
            throw new BizException("verify wechat signature failed: " + ex.getMessage());
        }
    }

    public String decryptNotifyResource(String associatedData, String nonce, String ciphertext) {
        ensureConfigForNotify();
        if (!StringUtils.hasText(nonce) || !StringUtils.hasText(ciphertext)) {
            throw new BizException("invalid notify resource payload");
        }
        String apiV3Key = properties.getApiV3Key();
        if (apiV3Key.getBytes(StandardCharsets.UTF_8).length != 32) {
            throw new BizException("api-v3-key must be 32 bytes");
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec key = new SecretKeySpec(apiV3Key.getBytes(StandardCharsets.UTF_8), "AES");
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            if (StringUtils.hasText(associatedData)) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }
            byte[] plain = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new BizException("decrypt notify resource failed: " + ex.getMessage());
        }
    }

    private String buildAuthorization(String method, String path, String body) {
        String nonceStr = UUID.randomUUID().toString().replace("-", "");
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String message = method + "\n" + path + "\n" + timestamp + "\n" + nonceStr + "\n" + body + "\n";
        String signature = sign(message);
        String token = "mchid=\"" + properties.getMchId()
                + "\",nonce_str=\"" + nonceStr
                + "\",timestamp=\"" + timestamp
                + "\",serial_no=\"" + properties.getMchSerialNo()
                + "\",signature=\"" + signature + "\"";
        return "WECHATPAY2-SHA256-RSA2048 " + token;
    }

    private String sign(String message) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(loadPrivateKey());
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception ex) {
            throw new BizException("wechat sign failed: " + ex.getMessage());
        }
    }

    private PrivateKey loadPrivateKey() throws Exception {
        String pem = properties.getPrivateKeyPem();
        String normalized = normalizePem(pem, "PRIVATE KEY");
        byte[] keyBytes = Base64.getDecoder().decode(normalized);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private PublicKey loadPlatformPublicKey() throws Exception {
        String pem = properties.getPlatformPublicKeyPem();
        String normalized = normalizePem(pem, "PUBLIC KEY");
        byte[] keyBytes = Base64.getDecoder().decode(normalized);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private String normalizePem(String pem, String type) {
        if (!StringUtils.hasText(pem)) {
            throw new BizException(type + " is empty");
        }
        return pem
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s+", "");
    }

    private String trimGateway() {
        String gateway = properties.getGateway();
        if (!StringUtils.hasText(gateway)) {
            throw new BizException("wechat gateway is empty");
        }
        if (gateway.endsWith("/")) {
            return gateway.substring(0, gateway.length() - 1);
        }
        return gateway;
    }

    private void ensureConfigForPay() {
        if (!StringUtils.hasText(properties.getAppId())
                || !StringUtils.hasText(properties.getMchId())
                || !StringUtils.hasText(properties.getMchSerialNo())
                || !StringUtils.hasText(properties.getPrivateKeyPem())
                || !StringUtils.hasText(properties.getNotifyUrl())) {
            throw new BizException("wechat pay config is incomplete");
        }
    }

    private void ensureConfigForNotify() {
        if (!StringUtils.hasText(properties.getPlatformPublicKeyPem())
                || !StringUtils.hasText(properties.getApiV3Key())) {
            throw new BizException("wechat notify config is incomplete");
        }
    }
}
