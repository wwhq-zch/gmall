package com.atguigu.gmall.index.config;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 切面类加强注解
 */
@Component
@Aspect
public class GmallCacheAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RBloomFilter rBloomFilter;

    /**
     * joinPoint.getArgs(); 获取方法参数
     * joinPoint.getTarget().getClass(); 获取目标类
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("@annotation(com.atguigu.gmall.index.config.GmallCache)") // 切入点表达式 execution(* com.atguigu.spring.aspectj.UserDao.delete(..)
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable{// 使用JoinPoint接口或其子接口ProceedingJoinPoint拦截方法，获取链接点信息
        // 使用布隆过滤器解决缓存穿透问题
        // 获取方法的参数
        List<Object> args = Arrays.asList(joinPoint.getArgs()); // [args...]
        String pid = args.get(0).toString();
        if (!this.rBloomFilter.contains(pid)){
            return null;
        }

        // 获取切点方法的签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // 获取方法对象
        Method method = signature.getMethod();
        // 获取方法上指定注解的对象
        GmallCache annotation = method.getAnnotation(GmallCache.class);
        // 获取方法注解中的参数
        String prefix = annotation.prefix();
        int timeout = annotation.timeout();
        int random = annotation.Random();
        String lock = annotation.lock();
        // 拼接缓存key
        String key = prefix + pid;
        // 获取方法的返回值类型
        Class<?> returnType = method.getReturnType();


        // 拦截前代码块：判断缓存中有没有
        String json = this.redisTemplate.opsForValue().get(key);
        // 判断缓存中的数据是否为空
        if (StringUtils.isNotBlank(json)){
            return JSON.parseObject(json, returnType);
        }
        // 没有，加分布式锁
        RLock rLock = this.redissonClient.getLock(lock + pid);
        rLock.lock();
        Object result = null;
        try {
            // 判断缓存中有没有，有直接返回(加锁的过程中，别的请求可能已经把数据放入缓存)
            String json2 = this.redisTemplate.opsForValue().get(key);
            if (StringUtils.isNotBlank(json2)){
                return JSON.parseObject(json2, returnType);
            }

            // 执行目标方法
            result = joinPoint.proceed(joinPoint.getArgs());

            // 拦截后代码块：放入缓存 释放分布锁
            this.redisTemplate.opsForValue().set(key,JSON.toJSONString(result), timeout + new Random().nextInt(random), TimeUnit.MINUTES);

        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            rLock.unlock(); // 释放锁 为了安全，释放资源的操作一般放在finally中
        }
        return result;
    }


}
