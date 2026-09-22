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
@TableName("quote_cache")
public class QuoteCache {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String secid;

    private String name;

    private BigDecimal price;

    private BigDecimal pctChg;

    private LocalDateTime quoteTs;
}
