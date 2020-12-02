package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.payment.entity.PaymentInfoEntity;
import com.atguigu.gmall.payment.vo.PayAsyncVo;

public interface PaymentService {
    OrderEntity queryOrderByToken(String orderToken);

    Long savePaymentInfo(OrderEntity orderEntity);

    PaymentInfoEntity queryById(String payId);

    int update(PayAsyncVo payAsyncVo);
}
