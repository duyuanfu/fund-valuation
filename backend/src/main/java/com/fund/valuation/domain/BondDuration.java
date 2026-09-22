package com.fund.valuation.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@TableName("bond_duration")
public class BondDuration {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fundCode;

    private String reportQt;

    private BigDecimal duration;

    private String source;
}