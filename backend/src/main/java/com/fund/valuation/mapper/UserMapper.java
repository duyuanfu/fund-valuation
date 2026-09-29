package com.fund.valuation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.valuation.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 数据库层分组聚合统计获客渠道分布，避免全表实体加载至内存。
     */
    @Select("SELECT referral_source AS source, COUNT(*) AS total FROM `user` GROUP BY referral_source")
    List<Map<String, Object>> countByReferralSource();
}