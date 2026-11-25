package com.fund.valuation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.valuation.client.TianTianFundClient;
import com.fund.valuation.client.TianTianFundClient.FundSearchItem;
import com.fund.valuation.domain.Fund;
import com.fund.valuation.mapper.FundMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class FundSearchService {

    private final FundMapper fundMapper;
    private final TianTianFundClient tianTianFundClient;

    private static final Pattern CLEAN_SUFFIX_PATTERN = Pattern.compile("(?i)(\\([^)]*\\)|（[^）]*）|LOF|ETF|联接|混合|股票|债券|指数|A类|C类|[AC])$");

    /**
     * 基金模糊搜索:
     * 1. 优先本地 fund 表匹配;
     * 2. 数量不足或未能精准命中时远端天天基金目录兜底;
     * 3. 智能清理冗余后缀 (如 (LOF)) 并根据 A/C 分额加权排序。
     */
    public List<FundSearchItem> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        int max = limit > 0 ? Math.min(limit, 30) : 10;
        String trimmed = query.trim();

        Map<String, FundSearchItem> candidates = new LinkedHashMap<>();

        // 1. 本地数据库查询
        try {
            List<Fund> localFunds = fundMapper.selectList(new LambdaQueryWrapper<Fund>()
                    .like(Fund::getCode, trimmed)
                    .or()
                    .like(Fund::getName, trimmed)
                    .last("LIMIT 20"));
            for (Fund f : localFunds) {
                candidates.put(f.getCode(), new FundSearchItem(f.getCode(), f.getName(), f.getTypeRaw() != null ? f.getTypeRaw() : f.getType()));
            }
        } catch (Exception e) {
            log.debug("local fund search error: {}", e.getMessage());
        }

        // 2. 远端天天基金兜底搜索
        if (candidates.size() < max) {
            try {
                List<FundSearchItem> remoteItems = tianTianFundClient.searchFundsByName(trimmed);
                for (FundSearchItem item : remoteItems) {
                    candidates.putIfAbsent(item.code(), item);
                }

                // 若候选依然偏少且原始名称含多余后缀 (如 LOF, 括号等)，剥离后二次检索
                if (candidates.size() < 3) {
                    String cleaned = cleanQuery(trimmed);
                    if (!cleaned.isBlank() && !cleaned.equalsIgnoreCase(trimmed)) {
                        List<FundSearchItem> secondary = tianTianFundClient.searchFundsByName(cleaned);
                        for (FundSearchItem item : secondary) {
                            candidates.putIfAbsent(item.code(), item);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("remote fund search error for {}: {}", trimmed, e.getMessage());
            }
        }

        // 3. 排序加权
        List<FundSearchItem> list = new ArrayList<>(candidates.values());
        boolean preferA = containsShareClassA(trimmed);
        boolean preferC = containsShareClassC(trimmed);
        String cleanedBase = cleanQuery(trimmed).toUpperCase();

        list.sort(Comparator.comparingInt((FundSearchItem item) -> {
            String code = item.code();
            String name = item.name().toUpperCase();

            // 代码完全匹配最高
            if (code.equalsIgnoreCase(trimmed)) return 0;
            // 名称完全匹配次高
            if (item.name().equalsIgnoreCase(trimmed)) return 10;
            // 代码前缀匹配
            if (code.startsWith(trimmed)) return 20;

            // 针对基准基金名称完全一致的 A/C 类份额提升至前排
            boolean isSameBase = !cleanedBase.isBlank() && name.contains(cleanedBase);

            if (preferA) {
                if (isSameBase && (name.endsWith("A") || name.contains("A类"))) return 30;
                if (isSameBase && (name.endsWith("C") || name.contains("C类"))) return 35;
            } else if (preferC) {
                if (isSameBase && (name.endsWith("C") || name.contains("C类"))) return 30;
                if (isSameBase && (name.endsWith("A") || name.contains("A类"))) return 35;
            } else {
                // 原截图中未注明 A/C，同名主基金的 A 类与 C 类平权优先并列置顶
                if (isSameBase && (name.endsWith("A") || name.contains("A类"))) return 30;
                if (isSameBase && (name.endsWith("C") || name.contains("C类"))) return 31;
            }

            if (isSameBase) return 40;
            if (name.contains(trimmed.toUpperCase())) return 50;
            return 100;
        }));

        if (list.size() > max) {
            return list.subList(0, max);
        }
        return list;
    }

    public String cleanQuery(String raw) {
        if (raw == null) return "";
        String s = raw.replaceAll("[\\(\\)\\[\\]（）]", " ").trim();
        s = CLEAN_SUFFIX_PATTERN.matcher(s).replaceAll("").trim();
        return s;
    }

    private boolean containsShareClassA(String query) {
        String upper = query.toUpperCase();
        return upper.endsWith("A") || upper.endsWith("A类") || upper.contains(" A");
    }

    private boolean containsShareClassC(String query) {
        String upper = query.toUpperCase();
        return upper.endsWith("C") || upper.endsWith("C类") || upper.contains(" C");
    }
}
