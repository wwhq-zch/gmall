package com.atguigu.gmall.ums.mapper;

import com.atguigu.gmall.ums.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表
 * 
 * @author zhangchanghong
 * @email zch@atguigu.com
 * @date 2020-09-26 21:28:05
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
	
}
