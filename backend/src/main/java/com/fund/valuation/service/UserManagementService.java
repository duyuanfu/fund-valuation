package com.fund.valuation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.valuation.domain.User;
import com.fund.valuation.domain.UserFund;
import com.fund.valuation.mapper.UserFundMapper;
import com.fund.valuation.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@DependsOn("databaseInitializer")
@RequiredArgsConstructor
public class UserManagementService {

    private final UserMapper userMapper;
    private final UserFundMapper userFundMapper;

    public record UserView(
            Long id,
            String username,
            String role,
            String status,
            boolean isVip,
            LocalDateTime vipExpireAt,
            LocalDateTime lastLoginAt,
            int loginCount,
            LocalDateTime createdAt,
            int watchlistCount
    ) {}

    public List<UserView> listUsers() {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .orderByDesc(User::getCreatedAt));
        List<UserFund> allFunds = userFundMapper.selectList(null);
        Map<String, Long> countMap = allFunds.stream()
                .collect(Collectors.groupingBy(UserFund::getUserId, Collectors.counting()));

        return users.stream().map(u -> new UserView(
                u.getId(),
                u.getUsername(),
                u.getRole() == null ? User.ROLE_USER : u.getRole(),
                u.getStatus() == null ? User.STATUS_NORMAL : u.getStatus(),
                u.isVipEffective(),
                u.getVipExpireAt(),
                u.getLastLoginAt(),
                u.getLoginCount() == null ? 0 : u.getLoginCount(),
                u.getCreatedAt(),
                countMap.getOrDefault(u.getUsername(), 0L).intValue()
        )).toList();
    }

    public void updateStatus(String username, String status) {
        if (!User.STATUS_NORMAL.equalsIgnoreCase(status) && !User.STATUS_DISABLED.equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("状态仅支持 NORMAL 或 DISABLED");
        }
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (u == null) {
            throw new IllegalArgumentException("用户不存在: " + username);
        }
        u.setStatus(status.toUpperCase());
        userMapper.updateById(u);
        log.info("user {} status updated to {}", username, status);
    }

    public void updateVip(String username, boolean isVip, LocalDateTime expireAt) {
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (u == null) {
            throw new IllegalArgumentException("用户不存在: " + username);
        }
        u.setIsVip(isVip);
        u.setVipExpireAt(isVip ? expireAt : null);
        userMapper.updateById(u);
        log.info("user {} vip updated to isVip={}, expireAt={}", username, isVip, expireAt);
    }

    public void updateRole(String username, String role) {
        if (!User.ROLE_ADMIN.equalsIgnoreCase(role) && !User.ROLE_USER.equalsIgnoreCase(role)) {
            throw new IllegalArgumentException("角色仅支持 ADMIN 或 USER");
        }
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (u == null) {
            throw new IllegalArgumentException("用户不存在: " + username);
        }
        u.setRole(role.toUpperCase());
        userMapper.updateById(u);
        log.info("user {} role updated to {}", username, role);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteUser(String username) {
        if ("admin".equalsIgnoreCase(username)) {
            throw new IllegalArgumentException("系统内置超级管理员账号不可删除");
        }
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (u == null) {
            throw new IllegalArgumentException("用户不存在: " + username);
        }
        userFundMapper.delete(new LambdaQueryWrapper<UserFund>().eq(UserFund::getUserId, username));
        userMapper.deleteById(u.getId());
        log.info("user {} and its watchlists deleted successfully", username);
    }
}
