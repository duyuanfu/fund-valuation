package com.fund.valuation.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("estimate_history")
public class EstimateHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fundCode;

    private LocalDateTime estTime;

    private BigDecimal estNav;

    private BigDecimal estPct;
}
