package com.atguigu.gmall.item.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {

    @Bean
    public ThreadPoolExecutor threadPoolExecutor(
            @Value("${threadPool.coreSize}") Integer coreSize,
            @Value("${threadPool.maxSize}") Integer maxSize,
            @Value("${threadPool.keepAlive}") Long keepAlive,
            @Value("${threadPool.blockingSize}") Integer blockingSize){
        return new ThreadPoolExecutor(coreSize, maxSize, keepAlive, TimeUnit.MINUTES, new ArrayBlockingQueue<>(blockingSize), Executors.defaultThreadFactory(), (Runnable r, ThreadPoolExecutor executor) -> {
            // 记录被拒绝的请求 比如日志
            System.out.println("您的请求被拒绝了");
        });
    }

}
