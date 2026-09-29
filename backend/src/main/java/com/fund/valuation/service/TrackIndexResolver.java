package com.fund.valuation.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 指数基金 -> 跟踪指数(secid)的解析。
 * 优先精确代码映射,其次按名称关键词推断。
 */
@Component
public class TrackIndexResolver {

    private static final Map<String, String> KEYWORD_INDEX = new LinkedHashMap<>();

    static {
        // 名称关键词 -> 指数 secid(与行情接口对齐)
        KEYWORD_INDEX.put("沪深300", "1.000300");
        KEYWORD_INDEX.put("中证800", "1.000906");
        KEYWORD_INDEX.put("中证1000", "1.000852");
        KEYWORD_INDEX.put("中证500", "1.000905");
        KEYWORD_INDEX.put("中证2000", "1.932000");
        KEYWORD_INDEX.put("上证50", "1.000016");
        KEYWORD_INDEX.put("上证180", "1.000010");
        KEYWORD_INDEX.put("科创50", "1.000688");
        KEYWORD_INDEX.put("科创100", "1.000698");
        KEYWORD_INDEX.put("中证红利", "1.000922");
        KEYWORD_INDEX.put("上证指数", "1.000001");
        KEYWORD_INDEX.put("深证100", "0.399330");
        KEYWORD_INDEX.put("深证成指", "0.399001");
        KEYWORD_INDEX.put("创业板", "0.399006");
        KEYWORD_INDEX.put("中证100", "1.000903");
        KEYWORD_INDEX.put("国证2000", "0.399303");
        KEYWORD_INDEX.put("央企", "1.000926");
        KEYWORD_INDEX.put("白酒", "0.399997");
        KEYWORD_INDEX.put("食品饮料", "1.000807");
        KEYWORD_INDEX.put("医药", "1.000933");
        KEYWORD_INDEX.put("消费", "1.000932");
        KEYWORD_INDEX.put("银行", "1.000986");
        KEYWORD_INDEX.put("证券", "0.399975");
        KEYWORD_INDEX.put("芯片", "0.399959");
        KEYWORD_INDEX.put("半导体", "0.399959");
        KEYWORD_INDEX.put("光伏", "0.399808");
        KEYWORD_INDEX.put("新能源", "1.000941");
        KEYWORD_INDEX.put("储能电池", "0.159566");
        KEYWORD_INDEX.put("储能", "0.159566");
        KEYWORD_INDEX.put("新能源车", "1.515700");
        KEYWORD_INDEX.put("新能车", "1.515700");
        KEYWORD_INDEX.put("电池", "0.159755");
        KEYWORD_INDEX.put("创新药", "0.159992");
        KEYWORD_INDEX.put("医疗", "1.512170");
        KEYWORD_INDEX.put("煤炭", "0.399998");
        KEYWORD_INDEX.put("传媒", "0.399971");
        KEYWORD_INDEX.put("计算机", "1.000935");
        KEYWORD_INDEX.put("黄金股", "0.159562");
        KEYWORD_INDEX.put("黄金产业", "0.159562");
        KEYWORD_INDEX.put("黄金", "1.518880");
        KEYWORD_INDEX.put("人工智能", "0.159819");
        KEYWORD_INDEX.put("AI", "0.159819");
        KEYWORD_INDEX.put("纳斯达克", "1.513100");
        KEYWORD_INDEX.put("恒生科技", "1.513180");
        KEYWORD_INDEX.put("游戏", "0.159869");
        KEYWORD_INDEX.put("军工", "1.512660");
        KEYWORD_INDEX.put("国防", "1.512660");
        KEYWORD_INDEX.put("有色", "1.512400");
    }

    /**
     * 解析跟踪指数 secid,无法识别返回 null。
     */
    public String resolve(String fundCode, String fundName) {
        if (fundName == null || fundName.isBlank()) {
            return null;
        }
        for (var e : KEYWORD_INDEX.entrySet()) {
            if (fundName.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }
}
