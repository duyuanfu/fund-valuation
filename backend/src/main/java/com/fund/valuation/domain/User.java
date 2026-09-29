package com.fund.valuation.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("`user`")
public class User {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";

    public static final String STATUS_NORMAL = "NORMAL";
    public static final String STATUS_DISABLED = "DISABLED";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String passwordHash;

    /** 角色: ADMIN / USER */
    private String role;

    /** 状态: NORMAL / DISABLED */
    private String status;

    /** 是否为 VIP */
    private Boolean isVip;

    /** VIP 过期时间 (null 且 isVip 为 true 表示永久有效) */
    private LocalDateTime vipExpireAt;

    /** 最后登录时间 */
    private LocalDateTime lastLoginAt;

    /** 累计登录次数 */
    private Integer loginCount;

    private LocalDateTime createdAt;

    /**
     * 判断当前用户是否处于有效 VIP 状态 (ADMIN 拥有全量超级权限，亦属于有效 VIP)。
     */
    public boolean isVipEffective() {
        if (ROLE_ADMIN.equalsIgnoreCase(role)) {
            return true;
        }
        if (Boolean.TRUE.equals(isVip)) {
            return vipExpireAt == null || vipExpireAt.isAfter(LocalDateTime.now());
        }
        return false;
    }
}
