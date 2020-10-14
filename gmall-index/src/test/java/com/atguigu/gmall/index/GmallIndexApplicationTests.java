package com.atguigu.gmall.index;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.CollectionUtils;

import java.util.List;

@SpringBootTest
class GmallIndexApplicationTests {

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private RBloomFilter<String> bloomFilter;

    @Test
    void testBloom() {
        // 谷歌提供的布隆过滤器
        BloomFilter<CharSequence> charSequenceBloomFilter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_8), 10, 0.03);
        charSequenceBloomFilter.put("1");
        charSequenceBloomFilter.put("2");
        charSequenceBloomFilter.put("3");
        charSequenceBloomFilter.put("4");
        charSequenceBloomFilter.put("5");
        System.out.println(charSequenceBloomFilter.mightContain("1"));
        System.out.println(charSequenceBloomFilter.mightContain("3"));
        System.out.println(charSequenceBloomFilter.mightContain("5"));
        System.out.println(charSequenceBloomFilter.mightContain("6"));
        System.out.println(charSequenceBloomFilter.mightContain("7"));
        System.out.println(charSequenceBloomFilter.mightContain("8"));
        System.out.println(charSequenceBloomFilter.mightContain("9"));
        System.out.println(charSequenceBloomFilter.mightContain("10"));
        System.out.println(charSequenceBloomFilter.mightContain("11"));
        System.out.println(charSequenceBloomFilter.mightContain("12"));
        System.out.println(charSequenceBloomFilter.mightContain("13"));
        System.out.println(charSequenceBloomFilter.mightContain("14"));
        System.out.println(charSequenceBloomFilter.mightContain("15"));
        System.out.println(charSequenceBloomFilter.mightContain("16"));
        System.out.println(charSequenceBloomFilter.mightContain("17"));
        System.out.println(charSequenceBloomFilter.mightContain("18"));

    }

    @Test
    void testRBoolFilter(){
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategory(0L);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();
        if (!CollectionUtils.isEmpty(categoryEntities)){
            categoryEntities.forEach(categoryEntity -> {
                bloomFilter.add(categoryEntity.getId().toString());
            });
        }
    }

}
