package com.daifu.manage.cart.controller;

import com.daifu.manage.cart.dto.AddCartItemRequest;
import com.daifu.manage.cart.dto.UpdateCartItemRequest;
import com.daifu.manage.cart.service.CartService;
import com.daifu.manage.cart.vo.CartItemVO;
import com.daifu.manage.cart.vo.CartListVO;
import com.daifu.manage.common.api.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/items")
    public Result<CartItemVO> add(@Valid @RequestBody AddCartItemRequest request) {
        return Result.ok(cartService.add(request));
    }

    @PutMapping("/items/{id}")
    public Result<CartItemVO> updateQuantity(@PathVariable Long id, @Valid @RequestBody UpdateCartItemRequest request) {
        return Result.ok(cartService.updateQuantity(id, request));
    }

    @DeleteMapping("/items/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        cartService.delete(id);
        return Result.ok();
    }

    @GetMapping
    public Result<CartListVO> listByUser(@RequestParam Long userId) {
        return Result.ok(cartService.listByUser(userId));
    }
}
