package com.fund.valuation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.valuation.config.AppProperties;
import com.fund.valuation.domain.BondDuration;
import com.fund.valuation.mapper.BondDurationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 债券基金组合久期管理。推断优先级: 表内记录 > 官方类型(FTYPE) > 名称关键词 > 配置默认。
 */
@Service
@RequiredArgsConstructor
public class BondDurationService {

    /** 官方类型(FTYPE)关键词 -> 组合久期(年),顺序敏感(优先匹配)。 */
    private static final Map<String, Double> DURATION_BY_TYPE_RAW = new LinkedHashMap<>();

    static {
        DURATION_BY_TYPE_RAW.put("中短债", 2.0);
        DURATION_BY_TYPE_RAW.put("短债", 1.5);
        DURATION_BY_TYPE_RAW.put("中长债", 4.0);
        DURATION_BY_TYPE_RAW.put("长债", 4.5);
        DURATION_BY_TYPE_RAW.put("信用债", 3.0);
        DURATION_BY_TYPE_RAW.put("利率债", 3.0);
        DURATION_BY_TYPE_RAW.put("纯债", 2.0);
        DURATION_BY_TYPE_RAW.put("中债", 2.0);
        DURATION_BY_TYPE_RAW.put("可转债", 3.0);
        DURATION_BY_TYPE_RAW.put("二级债", 3.0);
    }

    /** 名称关键词 -> 组合久期(年),顺序敏感(优先匹配)。 */
    private static final Map<String, Double> DURATION_BY_KEYWORD = new LinkedHashMap<>();

    static {
        DURATION_BY_KEYWORD.put("1-3年", 1.5);
        DURATION_BY_KEYWORD.put("1~3年", 1.5);
        DURATION_BY_KEYWORD.put("3-5年", 3.0);
        DURATION_BY_KEYWORD.put("3~5年", 3.0);
        DURATION_BY_KEYWORD.put("5-10年", 5.0);
        DURATION_BY_KEYWORD.put("5~10年", 5.0);
        DURATION_BY_KEYWORD.put("10年", 7.0);
        DURATION_BY_KEYWORD.put("中短债", 2.0);
        DURATION_BY_KEYWORD.put("短债", 1.5);
        DURATION_BY_KEYWORD.put("中长债", 4.0);
        DURATION_BY_KEYWORD.put("长债", 4.5);
        DURATION_BY_KEYWORD.put("国开", 3.0);
        DURATION_BY_KEYWORD.put("政金债", 3.0);
        DURATION_BY_KEYWORD.put("利率债", 3.0);
        DURATION_BY_KEYWORD.put("国债", 4.0);
        DURATION_BY_KEYWORD.put("地方债", 3.5);
        DURATION_BY_KEYWORD.put("信用债", 3.0);
        DURATION_BY_KEYWORD.put("二级", 3.0);
        DURATION_BY_KEYWORD.put("纯债", 2.0);
    }

    private final BondDurationMapper bondDurationMapper;
    private final AppProperties properties;

    /**
     * 取基金组合久期(年)。优先级: 表内记录 > 官方类型 > 名称关键词 > 配置默认。
     */
    public double getDuration(String fundCode, String fundName, String typeRaw) {
        BondDuration d = bondDurationMapper.selectOne(new LambdaQueryWrapper<BondDuration>()
                .eq(BondDuration::getFundCode, fundCode)
                .orderByDesc(BondDuration::getReportQt)
                .last("limit 1"));
        if (d != null) {
            return d.getDuration().doubleValue();
        }
        Double byType = match(DURATION_BY_TYPE_RAW, typeRaw);
        if (byType != null) {
            return byType;
        }
        Double byName = match(DURATION_BY_KEYWORD, fundName);
        if (byName != null) {
            return byName;
        }
        return properties.getBond().getDefaultDuration();
    }

    /**
     * 按官方类型推断久期,未命中返回 null。
     */
    public Double inferByTypeRaw(String typeRaw) {
        return match(DURATION_BY_TYPE_RAW, typeRaw);
    }

    /**
     * 按基金名称关键词推断久期,未命中返回 null。
     */
    public Double inferByKeyword(String fundName) {
        return match(DURATION_BY_KEYWORD, fundName);
    }

    private Double match(Map<String, Double> table, String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        for (Map.Entry<String, Double> e : table.entrySet()) {
            if (text.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * 维护久期记录(报告期变更覆盖)。
     */
    public void upsert(String fundCode, String reportQt, double duration, String source) {
        BondDuration existing = bondDurationMapper.selectOne(new LambdaQueryWrapper<BondDuration>()
                .eq(BondDuration::getFundCode, fundCode)
                .eq(BondDuration::getReportQt, reportQt)
                .last("limit 1"));
        if (existing != null) {
            existing.setDuration(BigDecimal.valueOf(duration).setScale(2, RoundingMode.HALF_UP));
            existing.setSource(source);
            bondDurationMapper.updateById(existing);
            return;
        }
        BondDuration d = new BondDuration();
        d.setFundCode(fundCode);
        d.setReportQt(reportQt);
        d.setDuration(BigDecimal.valueOf(duration).setScale(2, RoundingMode.HALF_UP));
        d.setSource(source);
        bondDurationMapper.insert(d);
    }
}