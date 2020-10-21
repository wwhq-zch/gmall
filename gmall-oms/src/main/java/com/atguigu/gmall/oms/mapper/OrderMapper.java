package com.atguigu.gmall.oms.mapper;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 订单
 * 
 * @author zhangchanghong
 * @email zch@atguigu.com
 * @date 2020-09-21 19:07:30
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {

    int closeOrder(@Param("orderToken") String orderToken);

    void updateStatus(@Param("orderToken") String orderToken, @Param("expect") int expect, @Param("target") int target);
}
