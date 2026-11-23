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
@TableName("sys_vip_order")
public class SysVipOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提交充值的用户名 */
    private String username;

    /** 套餐标识: month / quarter / year */
    private String planKey;

    /** 套餐名称: 月度会员 / 季度会员 / 年度会员 */
    private String planName;

    /** 支付金额 */
    private BigDecimal amount;

    /** 支付方式: wechat / alipay */
    private String payMethod;

    /** 订单状态: PENDING(待开通) / APPROVED(已开通) / REJECTED(已拒绝) */
    private String status;

    /** 充值通知提交时间 */
    private LocalDateTime createdAt;

    /** 管理员审批处理时间 */
    private LocalDateTime processedAt;
}
