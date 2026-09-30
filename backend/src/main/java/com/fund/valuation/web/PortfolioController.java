package com.fund.valuation.web;

import com.fund.valuation.config.JwtAuthFilter;
import com.fund.valuation.domain.User;
import com.fund.valuation.domain.UserPosition;
import com.fund.valuation.dto.PortfolioView;
import com.fund.valuation.dto.PositionSaveRequest;
import com.fund.valuation.dto.VipConfigView;
import com.fund.valuation.service.AuthService;
import com.fund.valuation.service.PortfolioService;
import com.fund.valuation.service.SysVipConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final AuthService authService;
    private final SysVipConfigService sysVipConfigService;

    /**
     * 获取VIP充值与价格配置 (供前台充值弹窗展示，无需VIP权限)。
     */
    @GetMapping("/vip-config")
    public ResponseEntity<VipConfigView> getVipConfig() {
        return ResponseEntity.ok(sysVipConfigService.getVipConfig());
    }

    /**
     * 获取当前用户的持仓列表与收益预估汇总 (VIP 专享)。
     */
    @GetMapping
    public ResponseEntity<PortfolioView> getPortfolio(HttpServletRequest request) {
        String username = currentUser(request);
        assertVip(username);
        return ResponseEntity.ok(portfolioService.getPortfolio(username));
    }

    /**
     * 新增或编辑持仓记录 (VIP 专享，对标支付宝4字段与智能联动)。
     */
    @PostMapping
    public ResponseEntity<?> save(HttpServletRequest request, @Valid @RequestBody PositionSaveRequest req) {
        String username = currentUser(request);
        assertVip(username);
        UserPosition pos = portfolioService.savePosition(username, req);
        return ResponseEntity.ok(Map.of("success", true, "id", pos.getId(), "fundCode", pos.getFundCode()));
    }

    /**
     * 删除单只持仓基金 (VIP 专享)。
     */
    @DeleteMapping("/{fundCode}")
    public ResponseEntity<?> remove(HttpServletRequest request, @PathVariable String fundCode) {
        String username = currentUser(request);
        assertVip(username);
        portfolioService.removePosition(username, fundCode);
        return ResponseEntity.ok(Map.of("success", true));
    }

    private void assertVip(String username) {
        if (username == null) {
            throw new SecurityException("请先登录账号");
        }
        User user = authService.getUser(username);
        if (user == null || !user.isVipEffective()) {
            throw new SecurityException("持仓收益实时估算为 VIP 会员专享特权，请开通 VIP 会员后使用");
        }
    }

    private String currentUser(HttpServletRequest request) {
        return (String) request.getAttribute(JwtAuthFilter.ATTR_USERNAME);
    }
}
