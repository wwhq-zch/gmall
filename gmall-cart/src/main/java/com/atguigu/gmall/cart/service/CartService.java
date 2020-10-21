package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.CartStatus;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.Future;

public interface CartService {
    void addCart(Cart cart);

    Cart queryCartBySkuId(Long skuId);

    String executor1();

    String executor2();

    List<Cart> queryCarts();

    void updateNum(Cart cart);

    void deleteCart(Long skuId);

    List<Cart> queryCheckedCarts(Long userId);

    void updateStatus(CartStatus cartStatus);

}
