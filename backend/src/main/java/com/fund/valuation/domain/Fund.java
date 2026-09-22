package com.fund.valuation.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("fund")
public class Fund {

    @TableId(type = IdType.INPUT)
    private String code;

    private String name;

    private String type;

    /** 官方类型原文(如"债券型-长债"),用于债券久期等细粒度推断 */
    private String typeRaw;

    private String trackIndex;

    private BigDecimal prevNav;

    private LocalDate navDate;

    private String freshFlag;

    private LocalDateTime updatedAt;
}
