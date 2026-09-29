package com.fund.valuation.service;

import com.fund.valuation.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 通用 VIP 权限与权益鉴权服务:
 * 供后续新功能 (如高级分时线、重仓透视、秒级行情等) 进行会员权限判定与拦截。
 */
@Service
@RequiredArgsConstructor
public class VipSecurityService {

    private final AuthService authService;

    /**
     * 判断指定用户是否为有效 VIP (ADMIN 拥有全量特权亦具备 VIP 权益)。
     */
    public boolean isVip(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        User u = authService.getUser(username);
        return u != null && u.isVipEffective() && !User.STATUS_DISABLED.equalsIgnoreCase(u.getStatus());
    }

    /**
     * 强校验 VIP 权限，非 VIP 用户直接抛出 SecurityException，被全局异常拦截器转化为 403 Forbidden。
     */
    public void requireVip(String username) {
        if (!isVip(username)) {
            throw new SecurityException("该功能为 VIP 会员专享，请联系管理员升级开通");
        }
    }
}
