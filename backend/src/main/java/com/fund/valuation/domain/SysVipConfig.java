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
@TableName("sys_vip_config")
public class SysVipConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private BigDecimal monthlyPrice;

    private BigDecimal quarterlyPrice;

    private BigDecimal quarterlyOrigPrice;

    private BigDecimal yearlyPrice;

    private BigDecimal yearlyOrigPrice;

    private String wechatQrUrl;

    private String alipayQrUrl;

    private String payeeName;

    private String paymentTip;

    /**
     * 新用户注册是否需要管理员审批: true需要 / false免审直接登录
     */
    private Boolean requireApproval;

    /**
     * 管理员接收VIP充值等消息的 Webhook 推送地址 (钉钉/企业微信/飞书/Server酱/Bark)
     */
    private String webhookUrl;

    private LocalDateTime updatedAt;
}
