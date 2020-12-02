package com.atguigu.gmall.common.bean;

import lombok.Data;

/**
 * 登录cookie，保存用户信息
 */
@Data
public class UserInfo {

    private Long userId;
    private String userKey;

}
