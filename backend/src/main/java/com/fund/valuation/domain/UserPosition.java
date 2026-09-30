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
@TableName("user_position")
public class UserPosition {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    private String fundCode;

    /**
     * 持仓总金额 / 资产现值 (元)
     */
    private BigDecimal holdingAmount;

    /**
     * 支付宝昨日实际结算收益 (元, 选填)
     */
    private BigDecimal yesterdayIncome;

    /**
     * 持有收益 / 累计收益 (元)
     */
    private BigDecimal holdingProfit;

    /**
     * 持有收益率 (%)
     */
    private BigDecimal holdingProfitRate;

    /**
     * 持仓成本 (元, 自动推算: holdingAmount - holdingProfit)
     */
    private BigDecimal costAmount;

    /**
     * 折算持有份额
     */
    private BigDecimal holdingShares;

    /**
     * 折算成本净值
     */
    private BigDecimal costPrice;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
