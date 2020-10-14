package com.atguigu.gmall.index.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BloomFilterConfig {

    @Autowired
    private RedissonClient redissonClient;

    /**
     * redisson提供的布隆过滤器
     * @return
     */
    @Bean
    public RBloomFilter rBloomFilter(){
        RBloomFilter<String> bloomFilter = this.redissonClient.getBloomFilter("bloomFilter");
        bloomFilter.tryInit(50L,0.03);
        return bloomFilter;
    }

}
