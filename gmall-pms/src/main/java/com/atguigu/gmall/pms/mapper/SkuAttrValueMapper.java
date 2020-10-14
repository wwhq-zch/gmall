package com.atguigu.gmall.pms.mapper;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.vo.AttrValueVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * sku销售属性&值
 * 
 * @author zhangchanghong
 * @email zch@atguigu.com
 * @date 2020-09-21 18:10:56
 */
@Mapper
public interface SkuAttrValueMapper extends BaseMapper<SkuAttrValueEntity> {

    List<AttrValueVo> querySkuAttrValuesBySpuId(Long spuId);

    List<Map<String, Object>> querySkuIdMappingSaleAttrValueBySpuId(Long spuId);

}
