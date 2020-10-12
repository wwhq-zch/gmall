package com.atguigu.gmall.index.service;

import com.atguigu.gmall.pms.entity.CategoryEntity;

import java.util.List;

public interface IndexService {

    /**
     * 获取一级分类
     * @return
     */
    List<CategoryEntity> queryLvl1Categories();


    /**
     * 根据一级分类id获取二三级分类
     * @param pid
     * @return
     */
    List<CategoryEntity> queryLvl2CategoriesWithSub(Long pid);

    /**
     * 测试本地锁的局限性及分布式锁的基本实现
     */
    void testLock();
}
