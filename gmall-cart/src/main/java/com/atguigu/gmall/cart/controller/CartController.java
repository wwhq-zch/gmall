package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.CartStatus;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.handler.GmallException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.SuccessCallback;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Api(description = "购物车 系统")
@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    @ApiOperation("获取登录用户勾选的购物车")
    @ResponseBody
    @GetMapping("check/{userId}")
    public ResponseVo<List<Cart>> queryCheckedCarts(@PathVariable("userId")Long userId){
        List<Cart> carts = this.cartService.queryCheckedCarts(userId);
        return ResponseVo.ok(carts);
    }

    @ApiOperation("添加购物车成功，重定向到购物车成功页")
    @GetMapping
    public String addCart(Cart cart){
        if (cart == null || cart.getSkuId() == null){
            throw new GmallException("没有选择添加到购物车的商品信息！");
        }
        this.cartService.addCart(cart);
        return "redirect:http://cart.gmall.com/addCart.html?skuId=" + cart.getSkuId();
    }

    @ApiOperation("跳转到添加成功页")
    @GetMapping("addCart.html")
    public String addCart(@RequestParam("skuId") Long skuId, Model model){
        Cart cart = this.cartService.queryCartBySkuId(skuId);
        model.addAttribute("cart", cart);
        return "addCart";
    }

    //    @ResponseBody
    @ApiOperation("查询购物车")
    @GetMapping("cart.html")
    public String queryCarts(Model model){
        List<Cart> carts = this.cartService.queryCarts();
        model.addAttribute("carts", carts);
        return "cart";
    }

    @ApiOperation("修改购物车商品选中状态")
    @ResponseBody
    @PostMapping("updateStatus")
    public ResponseVo<Object> updateStatus(@RequestBody CartStatus cartStatus){
        this.cartService.updateStatus(cartStatus);
        return ResponseVo.ok();
    }

    @ApiOperation("修改购物车商品数量")
    @ResponseBody
    @PostMapping("updateNum")
    public ResponseVo<Object> updateNum(@RequestBody Cart cart){
        this.cartService.updateNum(cart);
        return ResponseVo.ok();
    }

    @ApiOperation("删除购物车中的商品")
    @ResponseBody
    @PostMapping("deleteCart")
    public ResponseVo<Object> deleteCart(@RequestParam("skuId")Long skuId){
        this.cartService.deleteCart(skuId);
        return ResponseVo.ok();
    }


    /**
     * 测试mvc拦截器
     * @return
     */
    @GetMapping("test")
    @ResponseBody
    public String test(){
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        System.out.println(userInfo);
        return "hello cart!";
    }

    /**
     * springTask测试
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @GetMapping("test2")
    @ResponseBody
    public String test2() throws ExecutionException, InterruptedException {
        long now = System.currentTimeMillis();

        System.out.println("controller.test方法开始执行！");
        this.cartService.executor1();
        this.cartService.executor2();

//        System.out.println("controller.test方法开始执行！");
//        Future<String> future1 = this.cartService.executor1();
//        Future<String> future2 = this.cartService.executor2();
//        System.out.println("future1.get() = " + future1.get());
//        System.out.println("future2.get() = " + future2.get());

//        this.cartService.executor1().addCallback(new SuccessCallback<String>(){
//            @Override
//            public void onSuccess(String s) {
//                System.out.println("future1的正常执行结果：" + s);
//            }
//        },new FailureCallback(){
//            @Override
//            public void onFailure(Throwable throwable) {
//                System.out.println("future1执行出错：" + throwable.getMessage());
//            }
//        });
//
//        this.cartService.executor2().addCallback(new SuccessCallback<String>(){
//            @Override
//            public void onSuccess(String s) {
//                System.out.println("future2的正常执行结果：" + s);
//            }
//        },new FailureCallback(){
//            @Override
//            public void onFailure(Throwable throwable) {
//                System.out.println("future2执行出错：" + throwable.getMessage());
//            }
//        });

        System.out.println(System.currentTimeMillis() - now);

        return "hello cart!";
    }

}
