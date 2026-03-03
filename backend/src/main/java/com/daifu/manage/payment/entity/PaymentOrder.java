package com.daifu.manage.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_order")
public class PaymentOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private String openid;

    private BigDecimal totalAmount;

    private Integer status;

    private String channel;

    private String prepayId;

    private String wechatTransactionId;

    private LocalDateTime expireTime;

    private LocalDateTime payTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
