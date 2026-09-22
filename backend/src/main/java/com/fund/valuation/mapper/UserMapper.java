package com.fund.valuation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.valuation.domain.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}