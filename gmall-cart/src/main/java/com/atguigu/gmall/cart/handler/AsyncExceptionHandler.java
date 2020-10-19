package com.atguigu.gmall.cart.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
@Slf4j
public class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY = "cart:async:exception";

    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... objects) {
        log.error("异步调用发生异常，方法：{}，参数{}，异常信息：{}", method, objects, throwable.getMessage());

        // 把异常用户信息存入redis
        String userId = objects[0].toString();
        BoundListOperations<String, String> listOps = this.redisTemplate.boundListOps(KEY);// redis数据类型为list
        listOps.leftPush(userId);
    }
}
