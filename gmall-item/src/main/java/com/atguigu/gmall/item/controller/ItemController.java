package com.atguigu.gmall.item.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.item.vo.ItemVo;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Api(description = "商品详情 管理")
//@RestController
@Controller
@RequestMapping("item")
public class ItemController {

    @Autowired
    private ItemService itemService;

//    @ResponseBody
    @GetMapping("{skuId}.html")
    public String loadData(@PathVariable("skuId") Long skuId, Model model){
        ItemVo itemVo = this.itemService.loadData(skuId);
        model.addAttribute("itemVo", itemVo);
//        return ResponseVo.ok(itemVo);
        return "item";
    }
}
