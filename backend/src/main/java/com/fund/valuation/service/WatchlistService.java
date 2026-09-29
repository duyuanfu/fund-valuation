package com.fund.valuation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.valuation.domain.Fund;
import com.fund.valuation.domain.UserFund;
import com.fund.valuation.mapper.FundMapper;
import com.fund.valuation.mapper.UserFundMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

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

    @Autowired(required = false)
    @Qualifier("crawlerExecutor")
    private Executor crawlerExecutor;

    private void runAsyncCrawler(Runnable task) {
        if (crawlerExecutor != null) {
            CompletableFuture.runAsync(task, crawlerExecutor);
        } else {
            CompletableFuture.runAsync(task);
        }
    }

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

        // 异步在专属后台爬虫线程池中静默拉取持仓与资产配置，不阻塞通用计算线程
        runAsyncCrawler(() -> {
            try {
                fundHoldingService.refreshOne(fund);
            } catch (Exception e) {
                // 静默记录
            }
        });

        return uf;
    }

    /**
     * 批量添加自选: 支持一次提交多个基金代码，逐只独立处理并返回逐条结果。
     * 已在自选或代码无效的条目会被跳过并标记原因，不影响其余代码的添加。
     */
    public List<AddResult> addBatch(String userId, List<String> rawCodes) {
        List<AddResult> results = new ArrayList<>();
        if (rawCodes == null || rawCodes.isEmpty()) {
            return results;
        }

        List<String> codes = rawCodes.stream()
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();

        int sort = fundCodesOf(userId).size();
        for (String code : codes) {
            try {
                if (exists(userId, code)) {
                    results.add(new AddResult(code, false, "已在自选中"));
                    continue;
                }
                Fund fund = fundCatalogService.ensureFund(code);
                UserFund uf = new UserFund();
                uf.setUserId(userId);
                uf.setFundCode(code);
                uf.setSortNo(sort++);
                userFundMapper.insert(uf);

                final Fund added = fund;
                runAsyncCrawler(() -> {
                    try {
                        fundHoldingService.refreshOne(added);
                    } catch (Exception e) {
                        // 静默记录
                    }
                });

                results.add(new AddResult(code, true, "添加成功"));
            } catch (Exception e) {
                results.add(new AddResult(code, false, e.getMessage() == null ? "添加失败" : e.getMessage()));
            }
        }
        return results;
    }

    public record AddResult(String fundCode, boolean success, String message) {
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

        // 2. 一次性批量确保行情新鲜 (纯内存判定 + 缺失标的合并单次批量网络拉取)
        quoteService.ensureFreshQuotes(required);

        // 3. 纯内存装配估值结果
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