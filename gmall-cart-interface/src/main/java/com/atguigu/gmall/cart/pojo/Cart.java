package com.atguigu.gmall.cart.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车信息
 */
@Data
@TableName("cart_info") // 映射数据库表
public class Cart {

    @TableId
    private Long id;
    @TableField("user_id") // 与数据库字段名不一致时，声明字段映射
    private String userId;
    @TableField("sku_id")
    private Long skuId;
    @TableField("`check`") // check是mysql的关键字，所以这里要加'`'号
    private Boolean check; // 选中状态
    private String defaultImage;
    private String title;
    @TableField("sale_attrs")
    private String saleAttrs; // 销售属性：List<SkuAttrValueEntity>的json格式 方便存储到数据库中
    private BigDecimal price; // 加入购物车时的价格
    @TableField(exist = false) //声明该字段在mysql数据库不存在
    private BigDecimal currentPrice; // 实时价格，用于比价
    private BigDecimal count;
    private Boolean store = false; // 是否有货
    private String sales; // 营销信息: List<ItemSaleVo>的json格式 方便存储到数据库中
}
