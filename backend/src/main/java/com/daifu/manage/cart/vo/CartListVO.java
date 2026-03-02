package com.daifu.manage.cart.vo;

import java.util.List;

public record CartListVO(
        List<CartItemVO> items,
        CartSummaryVO summary
) {
}
