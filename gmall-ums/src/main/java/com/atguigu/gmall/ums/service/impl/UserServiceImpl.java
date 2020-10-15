package com.atguigu.gmall.ums.service.impl;

import com.atguigu.gmall.common.handler.GmallException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.ums.mapper.UserMapper;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.ums.service.UserService;


@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final static String KEY_PREFIX = "code:";

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<UserEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<UserEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public Boolean checkData(String data, Integer type) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        switch (type){
            case 1:
                queryWrapper.eq("username", data);
                break;
            case 2:
                queryWrapper.eq("phone", data);
                break;
            case 3:
                queryWrapper.eq("email", data);
                break;
            default:
                return null;
        }
        return this.userMapper.selectCount(queryWrapper) == 0;
    }

    @Override
    public void register(UserEntity userEntity, String code) {
        // 校验短信验证码（从redis中获取）
//        String cacheCode = this.redisTemplate.opsForValue().get(KEY_PREFIX + userEntity.getPhone());
//        if (!StringUtils.equals(code, cacheCode)) {
//            throw new GmallException("您的验证码有误！");
//        }

        // 生成盐
        String salt = StringUtils.replace(UUID.randomUUID().toString(), "-", "");
        userEntity.setSalt(salt);

        // 对密码加密 加盐
        userEntity.setPassword(DigestUtils.md5Hex(userEntity.getPassword() + salt));

        // 用户初始化
        userEntity.setCreateTime(new Date());
        userEntity.setLevelId(1L);
        userEntity.setStatus(1);
        userEntity.setIntegration(0);
        userEntity.setGrowth(0);
        userEntity.setNickname(userEntity.getUsername());

        // 添加到数据库
        boolean b = this.save(userEntity);

        // 注册成功，删除redis中的记录
//        if (b){
//            this.redisTemplate.delete(KEY_PREFIX + userEntity.getPhone());
//        }

    }

    @Override
    public UserEntity queryUser(String data, String password) {
        // 1.根据登录名查询用户信息（拿到盐）
        UserEntity userEntity = this.getOne(new QueryWrapper<UserEntity>()
                .eq("username", data)
                .or().eq("phone", data)
                .or().eq("email", data));

        // 2.判断用户是否为空
        if (userEntity == null) {
            throw new GmallException("账号或密码输入错误！");
        }

        // 3.对密码加盐加密，并和数据库中的密码进行比较
        if (!StringUtils.equals(DigestUtils.md5Hex(password + userEntity.getSalt()), userEntity.getPassword())){
            throw new GmallException("账号或密码输入错误！");
        }

        // 4.返回用户信息
        return userEntity;
    }
}