package com.atguigu.gmall.index.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Api(description = "商城首页 系统")
@Controller
public class IndexController {

    @Autowired
    private IndexService indexService;

    @ApiOperation("首页跳转")
    @GetMapping
    public String toIndex(Model model){

        List<CategoryEntity> categoryEntities = this.indexService.queryLvl1Categories();
        model.addAttribute("categories",categoryEntities);
        // TODO: 加载其他数据和打广告
        return "index";
    }

    @ApiOperation("获取二三分类")
    @ResponseBody
    @GetMapping("index/cates/{pid}")
    public ResponseVo<List<CategoryEntity>> queryLvl2CategoriesWithSub(@PathVariable("pid")Long pid){

        List<CategoryEntity> categoryEntities = this.indexService.queryLvl2CategoriesWithSub(pid);
        return ResponseVo.ok(categoryEntities);
    }

    @GetMapping("index/testlock")
    @ResponseBody
    public ResponseVo<Object> testLock() {
        this.indexService.testLock();
        return ResponseVo.ok(null);
    }
}
