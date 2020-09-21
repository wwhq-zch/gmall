package com.atguigu.gmall.oms.mapper;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author zhangchanghong
 * @email zch@atguigu.com
 * @date 2020-09-21 19:07:30
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {
	
}
