package com.daifu.manage.cart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.daifu.manage.cart.dto.AddCartItemRequest;
import com.daifu.manage.cart.dto.UpdateCartItemRequest;
import com.daifu.manage.cart.entity.CartItem;
import com.daifu.manage.cart.mapper.CartItemMapper;
import com.daifu.manage.cart.vo.CartItemVO;
import com.daifu.manage.cart.vo.CartListVO;
import com.daifu.manage.cart.vo.CartSummaryVO;
import com.daifu.manage.common.exception.BizException;
import com.daifu.manage.product.entity.Product;
import com.daifu.manage.product.mapper.ProductMapper;
import com.daifu.manage.user.service.UserService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final UserService userService;

    public CartService(CartItemMapper cartItemMapper, ProductMapper productMapper, UserService userService) {
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
        this.userService = userService;
    }

    public CartItemVO add(AddCartItemRequest request) {
        ensureUserExists(request.userId());
        Product product = getProductForCart(request.productId());

        CartItem existing = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, request.userId())
                .eq(CartItem::getProductId, request.productId())
                .last("limit 1"));

        if (existing == null) {
            CartItem item = new CartItem();
            item.setUserId(request.userId());
            item.setProductId(request.productId());
            item.setQuantity(request.quantity());
            item.setUnitPrice(product.getPrice());
            item.setTotalAmount(product.getPrice().multiply(BigDecimal.valueOf(request.quantity())));
            assertStockEnough(product, item.getQuantity());
            cartItemMapper.insert(item);
            return toVO(item, product);
        }

        int quantity = existing.getQuantity() + request.quantity();
        assertStockEnough(product, quantity);
        existing.setQuantity(quantity);
        existing.setUnitPrice(product.getPrice());
        existing.setTotalAmount(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        cartItemMapper.updateById(existing);
        return toVO(existing, product);
    }

    public CartItemVO updateQuantity(Long id, UpdateCartItemRequest request) {
        CartItem item = cartItemMapper.selectById(id);
        if (item == null) {
            throw new BizException("cart item not found");
        }
        Product product = getProductForCart(item.getProductId());
        assertStockEnough(product, request.quantity());
        item.setQuantity(request.quantity());
        item.setUnitPrice(product.getPrice());
        item.setTotalAmount(product.getPrice().multiply(BigDecimal.valueOf(request.quantity())));
        cartItemMapper.updateById(item);
        return toVO(item, product);
    }

    public void delete(Long id) {
        if (cartItemMapper.selectById(id) == null) {
            throw new BizException("cart item not found");
        }
        cartItemMapper.deleteById(id);
    }

    public CartListVO listByUser(Long userId) {
        ensureUserExists(userId);
        List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getId));

        Set<Long> productIds = items.stream().map(CartItem::getProductId).collect(Collectors.toSet());
        Map<Long, Product> productMap = productIds.isEmpty() ? Map.of() :
                productMapper.selectBatchIds(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<CartItemVO> records = items.stream()
                .map(item -> toVO(item, productMap.get(item.getProductId())))
                .toList();

        int totalQuantity = records.stream()
                .mapToInt(CartItemVO::quantity)
                .sum();
        BigDecimal totalAmount = records.stream()
                .map(CartItemVO::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartListVO(records, new CartSummaryVO(records.size(), totalQuantity, totalAmount));
    }

    private void ensureUserExists(Long userId) {
        if (userId == null || !userService.existsEnabledUser(userId)) {
            throw new BizException("user not found or disabled");
        }
    }

    private Product getProductForCart(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            throw new BizException("product not available");
        }
        return product;
    }

    private void assertStockEnough(Product product, int quantity) {
        if (product.getStock() == null || product.getStock() < quantity) {
            throw new BizException("insufficient stock");
        }
    }

    private CartItemVO toVO(CartItem item, Product product) {
        return new CartItemVO(
                item.getId(),
                item.getUserId(),
                item.getProductId(),
                product == null ? "" : product.getName(),
                product == null ? null : product.getStoreId(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalAmount(),
                item.getUpdateTime()
        );
    }
}
