package com.daifu.manage.store.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("store")
public class Store {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String contactName;

    private String contactPhone;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
