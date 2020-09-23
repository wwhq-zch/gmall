package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class SpuAttrValueVo extends SpuAttrValueEntity {

    private List<String> valueSelected;

    // 属性名与表字段名不一致，重写set方法
    public void setValueSelected(List<String> valueSelected) {
        if(valueSelected == null){
            return;
        }
        this.setAttrValue(StringUtils.join(valueSelected,","));
    }
}
