package com.daifu.manage.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pay_notify_log")
public class PayNotifyLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private String notifyData;

    private Integer notifyStatus;

    private LocalDateTime createTime;
}
