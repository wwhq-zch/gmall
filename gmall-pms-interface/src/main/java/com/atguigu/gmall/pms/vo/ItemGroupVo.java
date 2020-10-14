package com.atguigu.gmall.pms.vo;

import lombok.Data;

import java.util.List;

@Data
public class ItemGroupVo {

    private Long id;
    private String groupName;
    private List<AttrValueVo> attrValues;
}
