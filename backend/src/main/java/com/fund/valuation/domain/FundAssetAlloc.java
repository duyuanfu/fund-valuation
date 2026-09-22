package com.fund.valuation.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@TableName("fund_asset_alloc")
public class FundAssetAlloc {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fundCode;

    private LocalDate reportDate;

    private BigDecimal stockPct;

    private BigDecimal bondPct;

    private BigDecimal cashPct;
}
