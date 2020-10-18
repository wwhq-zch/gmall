package com.atguigu.gmall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.handler.GmallException;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.util.concurrent.FutureCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.ListenableFuture;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private CartAsyncService cartAsyncService;

    private static final String KEY_PREFIX = "cart:info:";
    private static final String PRICE_PREFIX = "cart:price:";

    @Override
    public void addCart(Cart cart) {
        // 1.获取登录信息
        String userId = getUserId();
        String key = KEY_PREFIX + userId;

        // 2.获取redis中该用户的购物车
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key); // 注意key为String类型

        // 3.判断该用户的购物车信息是否已包含了该商品
        String skuId = cart.getSkuId().toString();
        BigDecimal count = cart.getCount(); // 用户添加购物的商品数量
        if (hashOps.hasKey(skuId)){
            // 4.包含，更新数量
            String cartJson = hashOps.get(skuId).toString();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(cart.getCount().add(count)); // 注意数量的类型为BigDecimal

            // 异步执行更新操作
            this.cartAsyncService.updateCartByUserIdAndSkuId(userId, cart);

        } else {
            // 5.不包含，给该用户新增购物车记录 skuId count
            cart.setUserId(userId);

            // 根据skuId查询sku
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if ( skuEntity != null){
                cart.setTitle(skuEntity.getTitle());
                cart.setPrice(skuEntity.getPrice());
                cart.setDefaultImage(skuEntity.getDefaultImage());
            }
            // 根据skuId查询销售属性
            ResponseVo<List<SkuAttrValueEntity>> skuattrValueResponseVo = this.pmsClient.querySkuAttrValueBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = skuattrValueResponseVo.getData();
            cart.setSaleAttrs(JSON.toJSONString(skuAttrValueEntities));
            // 根据skuId查询营销信息
            ResponseVo<List<ItemSaleVo>> itemSaleVoResposneVo = this.smsClient.querySalesBySkuId(cart.getSkuId());
            List<ItemSaleVo> itemSaleVos = itemSaleVoResposneVo.getData();
            cart.setSales(JSON.toJSONString(itemSaleVos));
            // 根据skuId查询库存信息
            ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryWareSkuBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)){
                cart.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity ->
                    wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0
                ));
            }
            // 商品刚加入购物车时，默认为选中状态
            cart.setCheck(true);

            // mysql异步新增一个购物车记录
            this.cartAsyncService.saveCart(userId, cart);

            // 缓存实时价格
            this.redisTemplate.opsForValue().set(PRICE_PREFIX + skuId, skuEntity.getPrice().toString());

        }
        hashOps.put(skuId,JSON.toJSONString(cart)); // 执行时，会将购物车信息以hash的格式添加到redis缓存中
    }


    @Override
    public Cart queryCartBySkuId(Long skuId) {
        // 1.获取登录信息
        String userId = getUserId();
        String key = KEY_PREFIX + userId;

        // 2.获取redis中该用户的购物车
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        if (hashOps.hasKey(skuId.toString())) {
            String cartJson = hashOps.get(skuId.toString()).toString();
            return JSON.parseObject(cartJson, Cart.class);
        }
        throw new GmallException("您的购物车中没有该商品记录！");
    }

    /**
     * 获取用户的id
     * @return
     */
    private String getUserId() {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        if (userInfo.getUserId() != null){
            // 如果用户的id不为空，说明该用户已登录，添加购物车应该以userId作为key
            return userInfo.getUserId().toString(); // 注意userId应该转换为String类型
        }
        // 否则，说明用户未登录，以userKey作为key
        return userInfo.getUserKey();
    }

    @Override
    public List<Cart> queryCarts() {

        // 查询用户的信息，未登录的游客信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userKey = userInfo.getUserKey();

        // 1. 查询未登录的购物车
        String unLoginKey = KEY_PREFIX + userKey;
        // 获取了未登录的购物车
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(unLoginKey);
        // 获取未登录购物车的json集合
        List<Object> cartJsons = hashOps.values();
        List<Cart> unLoginCarts = null;
        // 反序列化为cart集合
        if (!CollectionUtils.isEmpty(cartJsons)){
            unLoginCarts = cartJsons.stream().map(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                // 查询实时价格
                String currentPriceString = this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId());
                cart.setCurrentPrice(new BigDecimal(currentPriceString));
                return cart;
            }).collect(Collectors.toList());
        }

        // 2. 判断是否登录，未登录直接返回
        Long userId = userInfo.getUserId();
        if(userId == null){
            return unLoginCarts;
        }

        // 3. 已登录合并购物车
        String loginKey = KEY_PREFIX + userId;
        // 获取了登录状态购物车操作对象
        BoundHashOperations<String, Object, Object> loginHashOps = this.redisTemplate.boundHashOps(loginKey);
        // 判断是否存在未登录的购物车，有则遍历未登录的购物车合并到已登录的购物车中去
        if (!CollectionUtils.isEmpty(unLoginCarts)){
            unLoginCarts.forEach(cart -> {
                // 登录状态购物车已存在该商品，更新数量
                if (loginHashOps.hasKey(cart.getSkuId().toString())){
                    // 未登录购物车当前商品的数量
                    BigDecimal count = cart.getCount();
                    // 获取登录状态的购物车并反序列化
                    String cartJson = loginHashOps.get(cart.getSkuId().toString()).toString();
                    cart = JSON.parseObject(cartJson, Cart.class);
                    // 更新登录状态的购物车
                    cart.setCount(cart.getCount().add(count));
                    // 异步更新到数据库
                    this.cartAsyncService.updateCartByUserIdAndSkuId(userId.toString(), cart);
                } else {
                    // 登录状态购物车不包含该记录，新增
                    cart.setUserId(userId.toString()); // 用userId覆盖掉userKey
                    this.cartAsyncService.saveCart(userId.toString(), cart);
                }
                loginHashOps.put(cart.getSkuId().toString(), JSON.toJSONString(cart));
            });
            // 合并完未登录的购物车之后，要删除未登录的购物车
            this.cartAsyncService.deleteCartByUserId(userKey);
            this.redisTemplate.delete(unLoginKey);
        }

        // 4.从redis中查询登录状态所有购物车信息，反序列化后返回
        List<Object> loginCartJsons = loginHashOps.values();
        if (!CollectionUtils.isEmpty(loginCartJsons)){
            return  loginCartJsons.stream().map(loginCartJson -> {
                Cart cart =  JSON.parseObject(loginCartJson.toString(), Cart.class);
                // 设置实时价格
                String currentPriceString = this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId().toString());
                cart.setCurrentPrice(new BigDecimal(currentPriceString));
                return cart;
            }).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public void updateNum(Cart cart) {
        String userId = this.getUserId();
        String key = KEY_PREFIX + userId;

        // 获取该用户的所有购物车
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        // 判断该用户的购物车中是否包含该条信息
        if (hashOps.hasKey(cart.getSkuId().toString())) {
            BigDecimal count = cart.getCount(); // 页面传递的要更新的数量
            String cartJson = hashOps.get(cart.getSkuId().toString()).toString();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(count);
            // 更新到mysql及redis
            this.cartAsyncService.updateCartByUserIdAndSkuId(userId, cart);
            hashOps.put(cart.getSkuId().toString(), JSON.toJSONString(cart));
        }

    }

    @Override
    public void deleteCart(Long skuId) {
        String userId = this.getUserId();
        String key = KEY_PREFIX + userId;

        // 获取该用户的所有购物车
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        // 判断该用户的购物车中是否包含该条信息
        if (hashOps.hasKey(skuId.toString())){
            // redis和（异步）mysql中存在删除该商品
            this.cartAsyncService.deleteCartByUserIdAndSkuId(userId, skuId);
            hashOps.delete(skuId.toString());
        }
    }

    /**
     * 测试springTask
     * @return
     */
    @Override
    @Async
    public String executor1() {
        try {
            System.out.println("executor1方法开始执行");
            TimeUnit.SECONDS.sleep(4);
            System.out.println("executor1方法执行结束");
            return "executor1";
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    @Async
    public String executor2() {
        try {
            System.out.println("executor2方法开始执行");
            TimeUnit.SECONDS.sleep(5);
            System.out.println("executor2方法执行结束");
            int i = 1 / 0;
            return "executor1";
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

//    @Override
//    @Async
//    public ListenableFuture<String> executor1() {
//        try {
//            System.out.println("executor1方法开始执行");
//            TimeUnit.SECONDS.sleep(4);
//            System.out.println("executor1方法执行结束");
//            return AsyncResult.forValue("executor1");
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//            return AsyncResult.forExecutionException(e);
//        }
//
//    }
//
//    @Override
//    @Async
//    public ListenableFuture<String> executor2() {
//        try {
//            System.out.println("executor2方法开始执行");
//            TimeUnit.SECONDS.sleep(5);
//            System.out.println("executor2方法执行结束");
//            int i = 1 / 0;
//            return AsyncResult.forValue("executor1");
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//            return AsyncResult.forExecutionException(e);
//        }
//
//    }

}
