package com.daifu.manage.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pay_order")
public class PayOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long productId;

    private String productName;

    private BigDecimal amount;

    private Integer status;

    private String payUrl;

    private LocalDateTime expireTime;

    private LocalDateTime payTime;

    private BigDecimal payAmount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
