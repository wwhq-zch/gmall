package com.atguigu.gmall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.handler.GmallException;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptor.LoginInterceptor;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private GmallUmsClient umsClient;
    @Autowired
    private GmallCartClient cartClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GmallOmsClient omsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;


    private static final String KEY_PREFIX = "order:token:";

    /**
     * 订单确认页
     * 由于存在大量的远程调用，这里使用异步编排做优化
     */
    @Override
    public OrderConfirmVo confirm() {
        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();

        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();

        // 查询送货清单
        CompletableFuture<List<Cart>> cartCompletableFuture = CompletableFuture.supplyAsync(() -> {
            ResponseVo<List<Cart>> cartResponseVo = this.cartClient.queryCheckedCarts(userId);
            List<Cart> carts = cartResponseVo.getData();
            if (!CollectionUtils.isEmpty(carts)) {
                return carts;
            }
            throw new GmallException("没有选中的购物车信息!");
        }, threadPoolExecutor);

        CompletableFuture<Void> itemCompletableFuture = cartCompletableFuture.thenAcceptAsync(carts -> {
            List<OrderItemVo> items = carts.stream().map(cart -> {
                OrderItemVo orderItemVo = new OrderItemVo();
                orderItemVo.setSkuId(cart.getSkuId());
                orderItemVo.setCount(cart.getCount().intValue());
                // 根据skuId查询sku
                CompletableFuture<Void> skuCompletableFuture = CompletableFuture.runAsync(() -> {
                    ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
                    SkuEntity skuEntity = skuEntityResponseVo.getData();
                    orderItemVo.setTitle(skuEntity.getTitle());
                    orderItemVo.setPrice(skuEntity.getPrice());
                    orderItemVo.setDefaultImage(skuEntity.getDefaultImage());
                    orderItemVo.setWeight(new BigDecimal(skuEntity.getWeight()));
                }, threadPoolExecutor);
                // 查询销售属性
                CompletableFuture<Void> saleAttrCompletableFuture = CompletableFuture.runAsync(() -> {
                    ResponseVo<List<SkuAttrValueEntity>> skuAttrValueResponseVo = this.pmsClient.querySkuAttrValueBySkuId(cart.getSkuId());
                    List<SkuAttrValueEntity> attrValueEntities = skuAttrValueResponseVo.getData();
                    orderItemVo.setSaleAttrs(attrValueEntities);
                }, threadPoolExecutor);
                // 根据skuId查询营销信息
                CompletableFuture<Void> saleCompletableFuture = CompletableFuture.runAsync(() -> {
                    ResponseVo<List<ItemSaleVo>> itemSaleVoResponseVo = this.smsClient.querySalesBySkuId(cart.getSkuId());
                    List<ItemSaleVo> itemSaleVos = itemSaleVoResponseVo.getData();
                    orderItemVo.setSales(itemSaleVos);
                }, threadPoolExecutor);
                // 根据 skuId查询库存信息
                CompletableFuture<Void> storeCompletableFuture = CompletableFuture.runAsync(() ->{
                    ResponseVo<List<WareSkuEntity>> wareSkuResponseVo = this.wmsClient.queryWareSkuBySkuId(cart.getSkuId());
                    List<WareSkuEntity> wareSkuEntities = wareSkuResponseVo.getData();
                    if (!CollectionUtils.isEmpty(wareSkuEntities)){
                           orderItemVo.setStore(wareSkuEntities.stream()
                                           .anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
                    }
                }, threadPoolExecutor);
                // 异步编排统一执行
                CompletableFuture.allOf(
                        skuCompletableFuture, saleAttrCompletableFuture, saleCompletableFuture, storeCompletableFuture).join();

                return orderItemVo;
            }).collect(Collectors.toList());

            orderConfirmVo.setItems(items);
        }, threadPoolExecutor);

        // 查询收货地址列表
        CompletableFuture<Void> addressCompletableFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<UserAddressEntity>> addressesResponseVo = this.umsClient.queryAddressesByUserId(userId);
            List<UserAddressEntity> addressEntities = addressesResponseVo.getData();
            orderConfirmVo.setAddresses(addressEntities);
        }, threadPoolExecutor);

        // 查询用户的积分信息
        CompletableFuture<Void> boundsCompletableFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUserById(userId);
            UserEntity userEntity = userEntityResponseVo.getData();
            if (userEntity != null) {
                orderConfirmVo.setBounds(userEntity.getIntegration());
            }
        }, threadPoolExecutor);

        // 防重的唯一标识
        CompletableFuture<Void> tokenCompletableFuture = CompletableFuture.runAsync(() -> {
            String timeId = IdWorker.getTimeId(); // mybatis-plus提供的id生成工具类，生成添加毫秒数的id
            this.redisTemplate.opsForValue().set(KEY_PREFIX + timeId, timeId);
            orderConfirmVo.setOrderToken(timeId);
        }, threadPoolExecutor);

        CompletableFuture.allOf(
                itemCompletableFuture, addressCompletableFuture, boundsCompletableFuture, tokenCompletableFuture).join();

        return orderConfirmVo;
    }

    @Override
    public OrderEntity submit(OrderSubmitVo submitVo) {

        // 1.防止订单重复提交
        String orderToken = submitVo.getOrderToken();
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                "then return redis.call('del', KEYS[1]) " +
                "else return 0 end"; // 使用lua脚本保证原子性
        Boolean flag = this.redisTemplate.execute(
                new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(KEY_PREFIX + orderToken), orderToken);
        if (!flag){
            throw new GmallException("您多次提交过快，请稍后再试！");
        }

        // 2.从数据查询总价，验证页面上的价格,重复验证价格，保证交易安全
        BigDecimal totalPrice = submitVo.getTotalPrice(); // 获取页面上的价格
        List<OrderItemVo> items = submitVo.getItems(); // 订单详情
        if (CollectionUtils.isEmpty(items)){
            throw new GmallException("您没有选中的商品，请选择要购买的商品！");
        }
        BigDecimal currentTotalPrice = items.stream().map(item -> {
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(item.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity != null) {
                return skuEntity.getPrice().multiply(new BigDecimal(item.getCount()));
            }
            return new BigDecimal(0);
        }).reduce((t1, t2) -> t1.add(t2)).get();
        if (totalPrice.compareTo(currentTotalPrice) != 0) {
            throw new GmallException("页面已过期，刷新后再试！");
        }

        // 3.验证库存，并锁定库存
        List<SkuLockVo> lockVos = items.stream().map(item -> {
            SkuLockVo skuLockVo = new SkuLockVo();
            skuLockVo.setSkuId(item.getSkuId());
            skuLockVo.setCount(item.getCount());
            skuLockVo.setOrderToken(submitVo.getOrderToken());
            return skuLockVo;
        }).collect(Collectors.toList());
        ResponseVo<List<SkuLockVo>> skuLockResponseVo = this.wmsClient.checkAndLock(lockVos);
        List<SkuLockVo> skuLockVos = skuLockResponseVo.getData();
        if (!CollectionUtils.isEmpty(skuLockVos)){
            throw new GmallException("手慢了，商品库存不足：" + JSON.toJSONString(skuLockVos));
        }

        // order：此时服务器宕机
//        int i = 1/0;

        // 4.下单
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();
        OrderEntity orderEntity = null;
        try {
            ResponseVo<OrderEntity> orderEntityResponseVo = this.omsClient.saveOrder(submitVo, userId);// feign（请求，响应）超时
            orderEntity = orderEntityResponseVo.getData();
        } catch (Exception e) {
            e.printStackTrace();
            // 如果订单创建失败，立马释放库存并更新订单状态为无效订单
            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.disable", orderToken);
            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "stock.unlock", orderToken);
        }

        // 5.删除购物车。异步发送消息给购物车，删除购物车
        HashMap<Object, Object> map = new HashMap<>();
        List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
        map.put("userId", userId);
        map.put("skuIds", skuIds);
        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "cart.delete", map);

        return orderEntity;
    }
}
