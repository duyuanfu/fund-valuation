package com.fund.valuation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.valuation.client.AssetAllocData;
import com.fund.valuation.client.HoldingData;
import com.fund.valuation.client.HoldingRow;
import com.fund.valuation.client.TianTianFundClient;
import com.fund.valuation.domain.Fund;
import com.fund.valuation.domain.FundAssetAlloc;
import com.fund.valuation.domain.FundHolding;
import com.fund.valuation.mapper.FundAssetAllocMapper;
import com.fund.valuation.mapper.FundHoldingMapper;
import com.fund.valuation.mapper.FundMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 持仓数据采集:前十大重仓股、资产配置,报告期变更检测。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FundHoldingService {

    private final FundMapper fundMapper;
    private final FundHoldingMapper holdingMapper;
    private final FundAssetAllocMapper allocMapper;
    private final TianTianFundClient fundClient;

    /**
     * 刷新全部基金的持仓与资产配置。
     * 非事务: 网络爬虫 I/O 绝不持有数据库长事务，避免耗尽 HikariCP 连接池。
     */
    public void refreshAll() {
        List<Fund> funds = fundMapper.selectList(null);
        for (Fund fund : funds) {
            try {
                refreshOne(fund);
            } catch (Exception e) {
                log.warn("refresh holding for {} failed: {}", fund.getCode(), e.getMessage());
            }
        }
    }

    public void refreshOne(Fund fund) {
        // 1. 场内基金直接在交易所撮合成交，不需要爬取网页重仓股票
        if (com.fund.valuation.common.SecCodeConverter.isOnMarket(fund.getCode(), fund.getName())) {
            return;
        }

        HoldingData holdings = fundClient.fetchTopHoldings(fund.getCode());
        if (holdings != null && holdings.rows() != null && !holdings.rows().isEmpty()) {
            upsertHoldings(fund.getCode(), holdings);
        }

        // 2. 只有混合型基金才依赖资产配置(股票/债券/现金比例)计算，指数/股票/纯债一律跳过，节省大量无谓的网络超时
        if (com.fund.valuation.common.FundType.MIXED.getCode().equals(fund.getType())) {
            try {
                AssetAllocData alloc = fundClient.fetchAssetAlloc(fund.getCode());
                if (alloc != null) {
                    upsertAlloc(fund.getCode(), alloc);
                }
            } catch (Exception e) {
                log.debug("fetch alloc skipped for {}: {}", fund.getCode(), e.getMessage());
            }
        }
    }

    @Transactional
    public void upsertHoldings(String fundCode, HoldingData data) {
        String reportDate = data.reportDate();
        List<FundHolding> existing = getHoldings(fundCode);
        if (!existing.isEmpty() && reportDate != null && reportDate.equals(existing.get(0).getReportQt())) {
            log.debug("holdings up-to-date for {}", fundCode);
            return;
        }
        holdingMapper.delete(new LambdaQueryWrapper<FundHolding>()
                .eq(FundHolding::getFundCode, fundCode));
        for (HoldingRow row : data.rows()) {
            FundHolding h = new FundHolding();
            h.setFundCode(fundCode);
            h.setStockCode(row.stockCode());
            h.setStockName(row.stockName());
            h.setWeight(row.weight());
            h.setReportQt(reportDate == null ? "" : reportDate);
            holdingMapper.insert(h);
        }
        log.info("updated holdings for {} (report {})", fundCode, reportDate);
    }

    @Transactional
    public void upsertAlloc(String fundCode, AssetAllocData data) {
        FundAssetAlloc existing = latestAlloc(fundCode);
        if (existing != null && existing.getReportDate().equals(data.reportDate())) {
            return;
        }
        allocMapper.delete(new LambdaQueryWrapper<FundAssetAlloc>()
                .eq(FundAssetAlloc::getFundCode, fundCode));
        FundAssetAlloc a = new FundAssetAlloc();
        a.setFundCode(fundCode);
        a.setReportDate(data.reportDate());
        a.setStockPct(defaultZero(data.stockPct()));
        a.setBondPct(defaultZero(data.bondPct()));
        a.setCashPct(defaultZero(data.cashPct()));
        allocMapper.insert(a);
        log.info("updated asset alloc for {} (report {})", fundCode, data.reportDate());
    }

    private BigDecimal defaultZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    public List<FundHolding> getHoldings(String fundCode) {
        return holdingMapper.selectList(new LambdaQueryWrapper<FundHolding>()
                .eq(FundHolding::getFundCode, fundCode)
                .le(FundHolding::getWeight, new BigDecimal("100.00")) // 防御过滤历史可能存在的抓取移位异常数据(如965.56%)
                .orderByDesc(FundHolding::getWeight));
    }

    public FundAssetAlloc latestAlloc(String fundCode) {
        return allocMapper.selectOne(new LambdaQueryWrapper<FundAssetAlloc>()
                .eq(FundAssetAlloc::getFundCode, fundCode)
                .orderByDesc(FundAssetAlloc::getReportDate)
                .last("limit 1"));
    }

    public String latestReportQt(String fundCode) {
        List<FundHolding> list = getHoldings(fundCode);
        return list.isEmpty() ? null : list.get(0).getReportQt();
    }
}