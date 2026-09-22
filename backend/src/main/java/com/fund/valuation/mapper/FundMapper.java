package com.fund.valuation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.valuation.domain.Fund;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FundMapper extends BaseMapper<Fund> {
}
