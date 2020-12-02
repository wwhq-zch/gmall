package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.payment.entity.PaymentInfoEntity;
import com.atguigu.gmall.payment.feign.GmallOmsClient;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.PaymentService;
import com.atguigu.gmall.payment.vo.PayAsyncVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private GmallOmsClient omsClient;

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Override
    public OrderEntity queryOrderByToken(String orderToken) {
        return this.omsClient.queryOrderByToken(orderToken).getData();
    }

    @Override
    public Long savePaymentInfo(OrderEntity orderEntity) {
        PaymentInfoEntity paymentInfoEntity = new PaymentInfoEntity();
        paymentInfoEntity.setPaymentStatus(0);
        paymentInfoEntity.setCreateTime(new Date());
        paymentInfoEntity.setTotalAmount(new BigDecimal(0.01));
        paymentInfoEntity.setSubject("谷粒商城支付订单");
        paymentInfoEntity.setPaymentType(orderEntity.getPayType());
        paymentInfoEntity.setOutTradeNo(orderEntity.getOrderSn());
        this.paymentInfoMapper.insert(paymentInfoEntity);
        return paymentInfoEntity.getId();
    }

    @Override
    public PaymentInfoEntity queryById(String payId) {
        return this.paymentInfoMapper.selectById(payId);
    }

    @Override
    public int update(PayAsyncVo payAsyncVo) {
        PaymentInfoEntity paymentInfoEntity = this.queryById(payAsyncVo.getPassback_params());
        paymentInfoEntity.setPaymentStatus(1);
        paymentInfoEntity.setTradeNo(payAsyncVo.getTrade_no());
        paymentInfoEntity.setCallbackTime(new Date());
        paymentInfoEntity.setCallbackContent(JSON.toJSONString(payAsyncVo));
        return this.paymentInfoMapper.updateById(paymentInfoEntity);
    }
}
