package com.daifu.manage.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.daifu.manage.payment.entity.PaymentOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {

    @Select("""
            SELECT *
            FROM payment_order
            WHERE order_no = #{orderNo}
            LIMIT 1
            FOR UPDATE
            """)
    PaymentOrder selectByOrderNoForUpdate(@Param("orderNo") String orderNo);
}
