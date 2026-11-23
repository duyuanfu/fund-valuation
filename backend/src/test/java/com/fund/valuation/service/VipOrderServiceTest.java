package com.fund.valuation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.valuation.domain.SysVipOrder;
import com.fund.valuation.domain.User;
import com.fund.valuation.dto.VipOrderNotifyRequest;
import com.fund.valuation.dto.VipOrderView;
import com.fund.valuation.mapper.SysVipOrderMapper;
import com.fund.valuation.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class VipOrderServiceTest {

    @Autowired
    private VipOrderService vipOrderService;

    @Autowired
    private SysVipOrderMapper vipOrderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AuthService authService;

    @BeforeEach
    void setup() {
        vipOrderMapper.delete(null);
        userMapper.delete(null);
    }

    @Test
    void testSubmitAndApproveOrder() {
        // 1. 注册普通用户 dyf
        authService.register("dyf", "password123");
        User dyf = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "dyf"));
        assertNotNull(dyf);
        dyf.setStatus(User.STATUS_NORMAL);
        dyf.setIsVip(false);
        dyf.setVipExpireAt(null);
        userMapper.updateById(dyf);

        // 2. 模拟 dyf 充值季度会员 (6.90 元)
        VipOrderNotifyRequest req = new VipOrderNotifyRequest("quarter", "季度会员", new BigDecimal("6.90"), "wechat");
        VipOrderView view = vipOrderService.submitOrder("dyf", req);
        assertNotNull(view);
        assertEquals("PENDING", view.status());
        assertEquals(new BigDecimal("6.90"), view.amount());
        assertEquals("季度会员", view.planName());

        // 3. 校验订单列表
        List<VipOrderView> orders = vipOrderService.listOrders();
        assertEquals(1, orders.size());
        assertEquals("dyf", orders.get(0).username());
        assertEquals("PENDING", orders.get(0).status());

        // 4. 管理员一键审批开通 VIP
        vipOrderService.approveOrder(view.id());

        // 5. 校验订单状态置为 APPROVED
        SysVipOrder approvedOrder = vipOrderMapper.selectById(view.id());
        assertNotNull(approvedOrder);
        assertEquals("APPROVED", approvedOrder.getStatus());
        assertNotNull(approvedOrder.getProcessedAt());

        // 6. 校验用户 dyf 的 VIP 状态与有效期已自动更新 (+90天)
        User updatedDyf = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "dyf"));
        assertNotNull(updatedDyf);
        assertTrue(updatedDyf.getIsVip());
        assertTrue(updatedDyf.isVipEffective());
        assertNotNull(updatedDyf.getVipExpireAt());
        assertTrue(updatedDyf.getVipExpireAt().isAfter(LocalDateTime.now().plusDays(85)));
    }
}
