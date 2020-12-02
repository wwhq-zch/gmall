package com.atguigu.gmall.cart.pojo;

import lombok.Data;

/**
 * 购物车商品当前状态（是否被选中）
 */
@Data
public class CartStatus {

    private Long skuId;
    private Boolean check;
}
