package com.atguigu.gmall.pms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.vo.AttrValueVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import org.springframework.util.CollectionUtils;


@Service("skuAttrValueService")
public class SkuAttrValueServiceImpl extends ServiceImpl<SkuAttrValueMapper, SkuAttrValueEntity> implements SkuAttrValueService {

    @Autowired
    private AttrMapper attrMapper;
    @Autowired
    private SkuAttrValueMapper attrValueMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuAttrValueEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuAttrValueEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<SkuAttrValueEntity> querySearchSkuAttrValuesByCidAndSkuId(Long cid, Long skuId) {
        // 根据分类id查询出检索类型的规格参数
        List<AttrEntity> attrEntities = this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("category_id", cid).eq("search_type", 1));
        if (CollectionUtils.isEmpty(attrEntities)){
            return null;
        }
        // 获取检索规格参数id,利用stream流转换集合对象
        List<Long> attrIds = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());

        // 根据skuId和attrIds查询销售检索类型的规格参数和值
        return this.list(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id",skuId).in("attr_id", attrIds));
    }

    @Override
    public List<SaleAttrValueVo> querySkuAttrValuesBySpuId(Long spuId) {
        List<AttrValueVo> attrValueVos = this.attrValueMapper.querySkuAttrValuesBySpuId(spuId);
        // 以attrId进行分组
        Map<Long, List<AttrValueVo>> map = attrValueVos.stream().collect(Collectors.groupingBy(AttrValueVo::getAttrId));

        // 创建一个List<SaleAttrValueVo>
        List<SaleAttrValueVo> saleAttrValueVos = new ArrayList<>();
        map.forEach((attrId, attrs) -> {
            SaleAttrValueVo saleAttrValueVo = new SaleAttrValueVo();
            saleAttrValueVo.setAttrId(attrId); // attrId
            saleAttrValueVo.setAttrName(attrs.get(0).getAttrName());// attrName
            Set<String> attrValues = attrs.stream().map(AttrValueVo::getAttrValue).collect(Collectors.toSet());
            saleAttrValueVo.setAttrValues(attrValues);// attrValues

            saleAttrValueVos.add(saleAttrValueVo);
        });


        return saleAttrValueVos;
    }

    @Override
    public String querySkuIdMappingSaleAttrValueBySpuId(Long spuId) {
        List<Map<String, Object>> maps = this.attrValueMapper.querySkuIdMappingSaleAttrValueBySpuId(spuId);

        Map<String, Long> jsonMap = maps.stream().collect(Collectors.toMap(map -> map.get("attr_values").toString(), map -> (Long)map.get("sku_id")));
        return JSON.toJSONString(jsonMap);
    }

}