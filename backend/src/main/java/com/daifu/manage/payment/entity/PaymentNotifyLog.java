package com.daifu.manage.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("payment_notify_log")
public class PaymentNotifyLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private String notifyId;

    private String eventType;

    private Integer verifyStatus;

    private String rawData;

    private LocalDateTime createTime;
}
