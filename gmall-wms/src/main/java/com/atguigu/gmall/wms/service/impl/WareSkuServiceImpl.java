package com.atguigu.gmall.wms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuMapper, WareSkuEntity> implements WareSkuService {

    @Autowired
    private WareSkuMapper wareSkuMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String KEY_PREFIX = "store:lock:";

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<WareSkuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageResultVo(page);
    }

    @Transactional
    @Override
    public List<SkuLockVo> checkAndLock(List<SkuLockVo> skuLockVos) {
        // 为空直接返回
        if (CollectionUtils.isEmpty(skuLockVos)){
            return null;
        }

        // 每一个商品验库存并锁库存
        skuLockVos.forEach(skuLockVo -> {
            this.checkLock(skuLockVo);
        });

        // 如果有一个商品锁定失败了，所有已经成功锁定的商品要解库存
        // 锁定成功的商品集合
        List<SkuLockVo> successLockVos = skuLockVos.stream().filter(SkuLockVo::getLock).collect(Collectors.toList());
        // 锁定失败的商品集合
        List<SkuLockVo> errorLockVos = skuLockVos.stream().filter(skuLockVo -> !skuLockVo.getLock()).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(errorLockVos)){
            // 存在锁定库存失败，回滚所有订单中商品的库存
            successLockVos.forEach(successLockVo -> {
                this.wareSkuMapper.unlockStock(successLockVo.getWareSkuId(), successLockVo.getCount());
            });
            return skuLockVos; // 返回原本的订单库存信息
        }

        // 把库存的锁定信息保存到redis中，以方便将来关单和解锁库存
        String orderToken = skuLockVos.get(0).getOrderToken();
        this.redisTemplate.opsForValue().set(KEY_PREFIX + orderToken, JSON.toJSONString(skuLockVos));

        // 定时解锁库存
        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE","stock.ttl", orderToken);

        return null; // 如果都锁定成功，不需要展示锁定情况
    }

    /**
     * 验证库存并锁定库存，使用分布式锁保证验库存和锁库存的原子性
     * @param skuLockVo
     */
    private void checkLock(SkuLockVo skuLockVo) {
        RLock fairLock = this.redissonClient.getFairLock("lock:" + skuLockVo);
        fairLock.lock();
        // 验库存
        List<WareSkuEntity> wareSkuEntities = this.wareSkuMapper.checkStock(skuLockVo.getSkuId(), skuLockVo.getCount());
        if (CollectionUtils.isEmpty(wareSkuEntities)){
            skuLockVo.setLock(false); // 库存不足，锁定失败
            fairLock.unlock(); // 程序返回之前，一定要释放锁
            return;
        }

        // 锁库存。一般会根据运输距离，就近调配。这里就锁定第一个仓库的库存
        if (this.wareSkuMapper.lockStock(wareSkuEntities.get(0).getId(), skuLockVo.getCount()) == 1){
            skuLockVo.setLock(true); // 锁定成功
            skuLockVo.setWareSkuId(wareSkuEntities.get(0).getId()); // 设置库存的id，未支付订单能方便解锁库存
        } else {
            skuLockVo.setLock(false);
        }

        fairLock.unlock();
    }
}