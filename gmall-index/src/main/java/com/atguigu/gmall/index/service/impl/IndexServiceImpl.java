package com.atguigu.gmall.index.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.config.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.index.utils.DistributedLock;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IndexServiceImpl implements IndexService {

    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    // 原生的redisTemplate使用jdkRedisSerializer序列化器
    // StringRedisTemplate使用stringRedisSerializer序列化器
    private StringRedisTemplate redisTemplate;
    @Autowired
    private DistributedLock distributedLock;
    @Autowired
    private RedissonClient redissonClient;

    private static final String KEY_PREFIX = "index:category:";

    /**
     * 获取一级分类
     *
     * @return
     */
    @Override
    public List<CategoryEntity> queryLvl1Categories() {
        // 从缓存中获取
        String cacheCategories = this.redisTemplate.opsForValue().get(KEY_PREFIX + 0);
        if (StringUtils.isNotBlank(cacheCategories)) {
            // 如果缓存中有，直接返回
            List<CategoryEntity> categoryEntities = JSON.parseArray(cacheCategories, CategoryEntity.class);
            return categoryEntities;
        }

        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategory(0L);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();
        this.redisTemplate.opsForValue().set(KEY_PREFIX + 0, JSON.toJSONString(categoryEntities), 30, TimeUnit.DAYS);

        return categoryEntities;
    }

    /**
     * 通过自定义注解实现
     * @param pid
     * @return
     */
    @Override
    @GmallCache(prefix = KEY_PREFIX, lock = "lock", timeout = 129600, Random = 7200)
    public List<CategoryEntity> queryLvl2CategoriesWithSub(Long pid) {

        // 把查询结果放入缓存
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesWithSub(pid);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();

        return categoryEntities;
    }

    /**
     * 通过一级分类id获取二三级分类
     * @param pid
     * @return
     */
    public List<CategoryEntity> queryLvl2CategoriesWithSub2(Long pid) {
        // 从缓存中获取
        String cacheCategories = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isNotBlank(cacheCategories)) {
            // 如果缓存中有，直接返回
            return JSON.parseArray(cacheCategories, CategoryEntity.class);
        }

        RLock lock = this.redissonClient.getLock("lock" + pid);
        lock.lock();
        List<CategoryEntity> categoryEntities;

        try {
            // 再次校验缓存中是否有数据
            String cacheCategories2 = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
            if (StringUtils.isNotBlank(cacheCategories2)) {
                // 如果缓存中有，直接返回
                return JSON.parseArray(cacheCategories2, CategoryEntity.class);

            }

            // 把查询结果放入缓存
            ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesWithSub(pid);
            categoryEntities = listResponseVo.getData();
            if (CollectionUtils.isEmpty(categoryEntities)) {
                // 这里已经解决了缓存穿透问题 数据库不存在的数据被访问时依旧缓存，不过保存时间应该尽量短
                this.redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntities), 5, TimeUnit.MINUTES);
            } else {
                // 这里为了解决缓存雪崩问题，要给缓存时间添加随机值
                this.redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntities), 30 + new Random(5).nextInt(), TimeUnit.DAYS);
            }
        } finally {
            lock.unlock();
        }

        return categoryEntities;
    }

    /**
     * 使用redisson实现分布式锁
     */
    @Override
    public void testLock() {
        RLock lock = null;
        try {
            // 获取锁
            lock = this.redissonClient.getLock("lock");
            lock.lock(10, TimeUnit.SECONDS);// 加锁

            // 查询redis中的num值
            String value = this.redisTemplate.opsForValue().get("num");
            // 没有该值return
            if (StringUtils.isBlank(value)) {
                this.redisTemplate.opsForValue().set("num", "1");
            }
            // 有值就转成int
            int num = Integer.parseInt(value);
            // 把redis中的num值+1
            this.redisTemplate.opsForValue().set("num", String.valueOf(++num));

        } finally {
            lock.unlock();// 释放锁 使用可重入锁无需手动解锁，到时间自动释放锁
        }


    }

    /**
     * 实现可重入性和自动续期
     */
    public void testLock3() {
        String uuid = UUID.randomUUID().toString();
        // 获取锁
        Boolean lock = this.distributedLock.tryLock("lock", uuid, 9L);
        if (lock) {
            // 查询redis中的num值
            String value = this.redisTemplate.opsForValue().get("num");
            // 没有该值return
            if (StringUtils.isBlank(value)) {
                this.redisTemplate.opsForValue().set("num", "1");
            }
            // 有值就转成int
            int num = Integer.parseInt(value);
            // 把redis中的num值+1
            this.redisTemplate.opsForValue().set("num", String.valueOf(++num));

            try {
                TimeUnit.SECONDS.sleep(60); // 睡眠60S测试续期功能
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 测试锁的可重入性
            this.testSubLock(uuid);
            // 释放锁
            this.distributedLock.unlock("lock", uuid);
        }

    }

    //  测试分布式锁的可重入
    public void testSubLock(String uuid) {
        this.distributedLock.tryLock("lock", uuid, 9L);
        System.out.println("测试分布式锁的可重入...");
        this.distributedLock.unlock("lock", uuid);
    }


    /**
     * lua脚本实现原子性，uuid用于防误删，过期时间用于防止死锁问题
     */
    public void testLock2() {
        // 1. 从redis中获取锁,setnx
        String uuid = UUID.randomUUID().toString();
        Boolean lock = this.redisTemplate.opsForValue().setIfAbsent("lock", uuid, 2, TimeUnit.SECONDS);
        if (lock) {

            // 查询redis中的num值
            String value = this.redisTemplate.opsForValue().get("num");
            // 没有该值return
            if (StringUtils.isBlank(value)) {
                this.redisTemplate.opsForValue().set("num", "1");
            }
            // 有值就转成int
            int num = Integer.parseInt(value);
            // 把redis中的num值+1
            this.redisTemplate.opsForValue().set("num", String.valueOf(++num));

            // 2. 释放锁 del 判断是否是自己的锁，自己的锁才释放
            // 使用lua脚本保证原子性
            String script = "if(redis.call('get',KEYS[1])==ARGV[1]) then return redis.call('del',KEYS[1]) else return 0 end";
            this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList("lock"), uuid);
//            if (StringUtils.equals(redisTemplate.opsForValue().get("lock"), uuid)){
//                this.redisTemplate.delete("lock");
//            }

        } else {
            // 3. 每隔50毫秒钟回调一次，再次尝试获取锁
            try {
                Thread.sleep(50);
                this.testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }
}
