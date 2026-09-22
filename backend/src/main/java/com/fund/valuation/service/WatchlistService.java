package com.fund.valuation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.valuation.domain.Fund;
import com.fund.valuation.domain.UserFund;
import com.fund.valuation.mapper.FundMapper;
import com.fund.valuation.mapper.UserFundMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 多用户自选基金管理与自选估值列表组装。
 */
@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final UserFundMapper userFundMapper;
    private final FundMapper fundMapper;
    private final FundCatalogService fundCatalogService;
    private final FundHoldingService fundHoldingService;
    private final ValuationEngine valuationEngine;
    private final QuoteService quoteService;

    /**
     * 添加自选,基金不存在时先采集基础元数据。
     * 优化点: 预先校验重复、异步后台拉取重仓股与资产配置，保证前端毫秒级极速响应，绝不卡死。
     */
    public UserFund add(String userId, String fundCode) {
        if (exists(userId, fundCode)) {
            throw new IllegalArgumentException("基金已在自选中: " + fundCode);
        }
        Fund fund = fundCatalogService.ensureFund(fundCode);
        UserFund uf = new UserFund();
        uf.setUserId(userId);
        uf.setFundCode(fundCode);
        uf.setSortNo(fundCodesOf(userId).size());
        userFundMapper.insert(uf);

        // 异步在后台静默拉取持仓与资产配置，避免让用户在网页端苦等十几秒
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                fundHoldingService.refreshOne(fund);
            } catch (Exception e) {
                // 静默记录
            }
        });

        return uf;
    }

    @Transactional
    public void remove(String userId, String fundCode) {
        userFundMapper.delete(new LambdaQueryWrapper<UserFund>()
                .eq(UserFund::getUserId, userId)
                .eq(UserFund::getFundCode, fundCode));
    }

    @Transactional
    public void reorder(String userId, List<String> fundCodes) {
        List<UserFund> ufs = userFundMapper.selectList(new LambdaQueryWrapper<UserFund>()
                .eq(UserFund::getUserId, userId)
                .orderByAsc(UserFund::getSortNo));
        for (UserFund uf : ufs) {
            int idx = fundCodes.indexOf(uf.getFundCode());
            if (idx >= 0) {
                uf.setSortNo(idx);
                userFundMapper.updateById(uf);
            }
        }
    }

    public boolean exists(String userId, String fundCode) {
        return userFundMapper.selectCount(new LambdaQueryWrapper<UserFund>()
                .eq(UserFund::getUserId, userId)
                .eq(UserFund::getFundCode, fundCode)) > 0;
    }

    public List<String> fundCodesOf(String userId) {
        return userFundMapper.selectList(new LambdaQueryWrapper<UserFund>()
                        .eq(UserFund::getUserId, userId)
                        .orderByAsc(UserFund::getSortNo)).stream()
                .map(UserFund::getFundCode).toList();
    }

    public List<UserFund> allUserFunds() {
        return userFundMapper.selectList(null);
    }

    public List<String> distinctUserIds() {
        return userFundMapper.selectList(null).stream()
                .map(UserFund::getUserId).distinct().toList();
    }

    /**
     * 组装自选估值列表。
     * 批量预处理行情，杜绝循环单只请求阻塞 HTTP 连接。
     */
    public List<EstimateResult> estimates(String userId) {
        List<String> fundCodes = fundCodesOf(userId);
        if (fundCodes.isEmpty()) {
            return List.of();
        }

        List<Fund> funds = new ArrayList<>();
        for (String code : fundCodes) {
            Fund f = fundCatalogService.get(code);
            if (f != null) {
                funds.add(f);
            }
        }

        // 1. 批量收集全量自选所需的行情标的
        java.util.Set<String> required = new java.util.HashSet<>();
        for (Fund f : funds) {
            java.util.Set<String> s = valuationEngine.collectRequiredSecids(f);
            if (s != null) {
                required.addAll(s);
            }
        }

        // 2. 检查哪些标的在内存缓存中缺失
        java.util.Set<String> missing = new java.util.HashSet<>();
        for (String secid : required) {
            if (quoteService.getFreshQuote(secid) == null) {
                missing.add(secid);
            }
        }

        // 3. 一次性单次批量刷新全部缺失标的（极速完成）
        if (!missing.isEmpty()) {
            quoteService.refreshQuotes(missing);
        }

        // 4. 纯内存装配估值结果
        List<EstimateResult> out = new ArrayList<>();
        for (Fund fund : funds) {
            EstimateResult r = valuationEngine.estimate(fund);
            if (r == null) {
                r = new EstimateResult(fund.getCode(), fund.getName(), fund.getType(),
                        null, null, null, null, null, null,
                        true, "无净值基准,无法估算", ValuationEngine.DISCLAIMER);
            }
            out.add(r);
        }
        return out;
    }
}