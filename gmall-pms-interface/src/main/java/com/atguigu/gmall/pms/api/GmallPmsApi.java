package com.atguigu.gmall.pms.api;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * openFeign最佳实践，提供一个接口工程，远程调用的服务和被调用服务都引入接口工程
 */
public interface GmallPmsApi {
    @PostMapping("pms/spu/json")
    public ResponseVo<List<SpuEntity>> querySpuByPageJson(@RequestBody PageParamVo paramVo);

    @GetMapping("pms/spu/{id}")
    public ResponseVo<SpuEntity> querySpuById(@PathVariable("id") Long id);

    @GetMapping("pms/sku/spu/{spuId}")
    public ResponseVo<List<SkuEntity>> querySkusBySpuId(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/sku/{id}")
    public ResponseVo<SkuEntity> querySkuById(@PathVariable("id") Long id);

    @GetMapping("pms/brand/{id}")
    public ResponseVo<BrandEntity> queryBrandById(@PathVariable("id") Long id);

    @GetMapping("pms/category/{id}")
    public ResponseVo<CategoryEntity> queryCategoryById(@PathVariable("id") Long id);

    @GetMapping("pms/category/parent/{parentId}")
    public ResponseVo<List<CategoryEntity>> queryCategory(@PathVariable("parentId")Long parentId);

    @GetMapping("pms/category/subs/{pid}")
    public ResponseVo<List<CategoryEntity>> queryCategoriesWithSub(@PathVariable("pid") Long pid);

    @GetMapping("pms/category/all/{cid3}")
    public ResponseVo<List<CategoryEntity>> queryCategoriesByCid3(@PathVariable("cid3") Long cid3);

    @GetMapping("pms/skuattrvalue/search/{cid}/{skuId}")
    public ResponseVo<List<SkuAttrValueEntity>> querySearchSkuAttrValuesByCidAndSkuId(
            @PathVariable("cid")Long cid, @PathVariable("skuId")Long skuId
    );

    @GetMapping("pms/skuattrvalue/spu/{spuId}")
    public ResponseVo<List<SaleAttrValueVo>> querySkuAttrValuesBySpuId(@PathVariable("spuId") Long spuId);

    @GetMapping("pms/skuattrvalue/sku/{skuId}")
    public ResponseVo<List<SkuAttrValueEntity>> querySkuAttrValueBySkuId(@PathVariable("skuId")Long skuId);

    @GetMapping("pms/skuattrvalue/spu/mapping/{spuId}")
    public ResponseVo<String> querySkuIdMappingSaleAttrValueBySpuId(@PathVariable("spuId") Long spuId);

    @GetMapping("pms/spuattrvalue/search/{cid}/{spuId}")
    public ResponseVo<List<SpuAttrValueEntity>> querySearchSpuAttrValuesByCidAndSpuId(
            @PathVariable("cid") Long cid, @PathVariable("spuId") Long spuId
    );

    @GetMapping("pms/skuimages/sku/{skuId}")
    public ResponseVo<List<SkuImagesEntity>> queryImagesBySkuId(@PathVariable("skuId") Long skuId);

    @GetMapping("pms/spudesc/{spuId}")
    public ResponseVo<SpuDescEntity> querySpuDescById(@PathVariable("spuId") Long spuId);

    @GetMapping("pms/attrgroup/with/attr/value/{categoryId}")
    public ResponseVo<List<ItemGroupVo>> queryGroupsWithAttrAndValueByCidAndSpuIdAndSkuId(
            @PathVariable("categoryId") Long categoryId,
            @RequestParam("spuId")Long spuId,
            @RequestParam("skuId")Long skuId);

}
