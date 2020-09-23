package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.List;

@Data
public class GroupVo extends AttrGroupEntity {

    @TableField(exist = false) // 注明该字段在数据库不存在
    private List<AttrEntity> attrEntities;
}
