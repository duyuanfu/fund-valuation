package com.fund.valuation.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("sys_email_config")
public class SysEmailConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** SMTP 服务器主机名，如 smtp.163.com, smtp.qq.com */
    private String smtpHost;

    /** SMTP 端口，SSL 默认 465，普通默认 25 */
    private Integer smtpPort;

    /** SMTP 登录用户名 */
    private String smtpUsername;

    /** SMTP 授权密码 / 客户端独立密码 */
    private String smtpPassword;

    /** 发件人显示名称，如：基金实时估值系统 */
    private String senderName;

    /** 发件人地址 */
    private String senderAddress;

    /** 是否开启 SSL */
    private Boolean sslEnabled;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
