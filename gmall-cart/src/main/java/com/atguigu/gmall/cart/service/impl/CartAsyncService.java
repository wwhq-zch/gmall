package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.pojo.Cart;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class CartAsyncService {

    @Autowired(required = false)
    private CartMapper cartMapper;

    /**
     * 为了方便将来在异常处理器中获取异常用户信息
     * 所有异步方法的第一个参数统一为userId
     * @param userId
     * @param cart
     */
    @Async
    public void updateCartByUserIdAndSkuId(String userId, Cart cart){
        this.cartMapper.update(cart,new UpdateWrapper<Cart>().eq("sku_id", cart.getSkuId()).eq("user_id", userId));

    }

    /**
     * 为了方便将来在异常处理器中获取异常用户信息
     * 所有异步方法的第一个参数统一为userId
     * @param userId
     * @param cart
     */
    @Async
    public void saveCart(String userId, Cart cart){
//        int i = 1 / 0;
        this.cartMapper.insert(cart);
    }

    /**
     * 为了方便将来在异常处理器中获取异常用户信息
     * 所有异步方法的第一个参数统一为userId
     * @param userId
     */
    @Async
    public void deleteCartByUserId(String userId) {
        this.cartMapper.delete(new QueryWrapper<Cart>().eq("user_id", userId));
    }

    /**
     * 为了方便将来在异常处理器中获取异常用户信息
     * 所有异步方法的第一个参数统一为userId
     * @param userId
     * @param skuId
     */
    @Async
    public void deleteCartByUserIdAndSkuId(String userId, Long skuId) {
        this.cartMapper.delete(new QueryWrapper<Cart>().eq("sku_id", skuId).eq("user_id", userId));
    }
}
