package com.atguigu.gmall.common.handler;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 自定义异常类
 */
@Data
@AllArgsConstructor // 自动生成全参构造器
@NoArgsConstructor // 自动生成无参构造器
public class GmallException extends RuntimeException{
//    @ApiModelProperty(value = "状态码")
//    private Integer code;
    @ApiModelProperty(value = "报错信息")
    private String msg;
}
