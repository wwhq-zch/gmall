package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import com.atguigu.gmall.pms.service.SkuImagesService;
import com.atguigu.gmall.pms.service.SpuAttrValueService;
import com.atguigu.gmall.pms.vo.SkuVo;
import com.atguigu.gmall.pms.vo.SpuAttrValueVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import com.atguigu.gmall.sms.api.GmallSmsApi;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SpuMapper;
import com.atguigu.gmall.pms.service.SpuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

    @Autowired
    private SpuDescMapper spuDescMapper;
    @Autowired
    private SpuAttrValueService spuAttrValueService;
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private SkuAttrValueService skuAttrValueService;
    @Autowired
    private SkuImagesService skuImagesService;
    @Autowired
    private GmallSmsApi gmallSmsApi;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public PageResultVo querySpu(PageParamVo pageParamVo, Long categoryId) {
        // 封装查询条件
        QueryWrapper<SpuEntity> wrapper = new QueryWrapper<>();
        // 如果分类id不为0，要根据分类id查，否则查全部
        if(categoryId != 0){
            wrapper.eq("category_id", categoryId);
        }
        // 如果用户输入了检索条件，根据检索条件查
        String key = pageParamVo.getKey();
        if(!StringUtils.isEmpty(key)){
            wrapper.and(t -> t.like("name",key).or().like("id", key) );
        }
        return new PageResultVo(this.page(pageParamVo.getPage(), wrapper));
    }

    /**
     * 添加spu
     * @param spuVo
     */
    @Override
    @GlobalTransactional
    public void bigSave(SpuVo spuVo) {
        // 一、保存spu相关信息
        // 1.1. 保存spu基本信息
        saveSpu(spuVo);
        Long spuId = spuVo.getId();// 获取新增后的spuId

        // 1.2. 保存spu的描述信息
        saveSpuDesc(spuVo, spuId);


        // 1.3. 保存spu的规格参数信息 stream流处理集合
        saveBaseAttr(spuVo, spuId);

        // 二、保存sku相关信息
        saveSku(spuVo, spuId);

        // 最后制造异常
//        int i = 1 / 0;
    }

    /**
     * 保存sku相关信息及营销信息
     * @param spuVo
     * @param spuId
     */
    private void saveSku(SpuVo spuVo, Long spuId) {
        List<SkuVo> skuVos = spuVo.getSkus();
        if (CollectionUtils.isEmpty(skuVos)){
            return;
        }
        skuVos.forEach(skuVo -> {
            // 2.1. 保存sku基本信息
            SkuEntity skuEntity = new SkuEntity();
            BeanUtils.copyProperties(skuVo,skuEntity);
            // 品牌和分类的id需要从spu中获取
            skuEntity.setBrandId(spuVo.getBrandId());
            skuEntity.setCatagoryId(spuVo.getCategoryId());
            // 获取图片列表(可能为空)
            List<String> images = skuVo.getImages();
            if (!CollectionUtils.isEmpty(images)){
                // 设置第一张图片作为默认图片(老司机带路，学会提前预判需求)
                skuEntity.setDefaultImage(skuEntity.getDefaultImage()==null ? images.get(0) : skuEntity.getDefaultImage());
            }
            skuEntity.setSpuId(spuId);
            this.skuMapper.insert(skuEntity);
            // 获取skuId
            Long skuId = skuEntity.getId();

            // 2.2. 保存sku图片信息
            if (!CollectionUtils.isEmpty(images)){
                String defaultImage = images.get(0);
                List<SkuImagesEntity> skuImageses = images.stream().map(image -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setDefaultStatus(StringUtils.equals(defaultImage, image) ? 1 : 0);
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setSort(0);
                    skuImagesEntity.setUrl(image);
                    return skuImagesEntity;
                }).collect(Collectors.toList());
                this.skuImagesService.saveBatch(skuImageses);
            }

            // 2.3. 保存(批量)sku的规格参数（销售属性）
            List<SkuAttrValueEntity> saleAttrs = skuVo.getSaleAttrs();
            saleAttrs.forEach(saleAttr ->{
                // 设置属性名，需要根据id查询AttrEntity
                saleAttr.setSort(0);
                saleAttr.setSkuId(skuId);
            });
            this.skuAttrValueService.saveBatch(saleAttrs);

            // 三、保存优惠相关信息(Feign远程调用sms服务)
            SkuSaleVo skuSaleVo = new SkuSaleVo();
            BeanUtils.copyProperties(skuVo,skuSaleVo);
            skuSaleVo.setSkuId(skuId);
            this.gmallSmsApi.saveSkuSaleInfo(skuSaleVo);

        });
    }

    /**
     * 保存spu基本属性信息
     * @param spuVo
     * @param spuId
     */
    private void saveBaseAttr(SpuVo spuVo, Long spuId) {
        // 用户可能为空的数据参数都应该在逻辑中加入判断是否为空，防止空指针异常
        List<SpuAttrValueVo> baseAttrs = spuVo.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)){
            List<SpuAttrValueEntity> spuAttrValueEntities = baseAttrs.stream().map(spuAttrValueVo -> {
                spuAttrValueVo.setSpuId(spuId);
                spuAttrValueVo.setSort(0);
                return spuAttrValueVo;
            }).collect(Collectors.toList());
            // 批量保存stream流处理后的spu基本属性
            this.spuAttrValueService.saveBatch(spuAttrValueEntities);
        }
    }

    /**
     * 保存spu描述信息（图片）
     * @param spuVo
     * @param spuId
     */
    @Transactional
    public void saveSpuDesc(SpuVo spuVo, Long spuId) {
        List<String> spuImages = spuVo.getSpuImages();
        SpuDescEntity spuDescEntity = new SpuDescEntity();
        // 注意：spu_info_desc表的主键是spu_id,需要在实体类中配置该主键不是自增主键
        spuDescEntity.setSpuId(spuId);
        // 把商品的图片描述，保存到spu详情中，图片地址以逗号进行分割
        spuDescEntity.setDecript(StringUtils.join(spuImages,","));
        this.spuDescMapper.insert(spuDescEntity);
    }

    /**
     * 保存spu基本信息
     * @param spuVo
     */
    private void saveSpu(SpuVo spuVo) {
        spuVo.setCreateTime(new Date());
        spuVo.setUpdateTime(spuVo.getCreateTime());
        this.save(spuVo);
    }

}