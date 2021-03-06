package com.atguigu.gmall.pms.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.CategoryMapper;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public List<CategoryEntity> queryCategoriesWithSub(Long pid) {
        return this.categoryMapper.queryCategoriesByPid(pid);
    }

    @Override
    public List<CategoryEntity> queryCategoriesByCid3(Long cid3) {

        // 根据三级分类的id查询三级分类
        CategoryEntity lvl3CategoryEntity = this.categoryMapper.selectById(cid3);
        // 根据三级分类的父id查询二级分类
        CategoryEntity lvl2CategoryEntity = this.categoryMapper.selectById(lvl3CategoryEntity.getParentId());
        // 根据二级分类的父id查询一级分类
        CategoryEntity lvl1CategoryEntity = this.categoryMapper.selectById(lvl2CategoryEntity.getParentId());

        return Arrays.asList(lvl1CategoryEntity, lvl2CategoryEntity, lvl3CategoryEntity);
    }

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<CategoryEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<CategoryEntity> queryCategory(Long parentId) {
        // 构造查询条件
        QueryWrapper<CategoryEntity> queryWrapper = new QueryWrapper<>();
        // 如果parentId为-1，说明用户没有传该字段，查询所有
        if(parentId != -1){
            queryWrapper.eq("parent_id",parentId);
        }
        return categoryMapper.selectList(queryWrapper);
    }

}