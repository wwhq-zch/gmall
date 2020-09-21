package com.atguigu.gmall.sms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.sms.entity.SkuLadderEntity;

import java.util.Map;

/**
 * 商品阶梯价格
 *
 * @author zhangchanghong
 * @email zch@atguigu.com
 * @date 2020-09-21 19:18:23
 */
public interface SkuLadderService extends IService<SkuLadderEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

