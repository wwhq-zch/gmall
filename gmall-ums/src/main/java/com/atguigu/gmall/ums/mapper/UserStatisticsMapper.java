package com.atguigu.gmall.ums.mapper;

import com.atguigu.gmall.ums.entity.UserStatisticsEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 统计信息表
 * 
 * @author zhangchanghong
 * @email zch@atguigu.com
 * @date 2020-09-26 21:28:05
 */
@Mapper
public interface UserStatisticsMapper extends BaseMapper<UserStatisticsEntity> {
	
}
