package com.fund.valuation.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 证券代码与东财 secid 的转换工具(用于持仓代码,如 600519/000001/113050)。
 * 规则:
 * - 沪市: 6xxxxx/5xxxxx/9xxxxx 及可转债 110/111/113/118xxx -> 1.xxxxxx
 * - 深市: 0xxxxx/3xxxxx 及可转债 123xxx/127xxx -> 0.xxxxxx
 * 注意: 指数代码(如 000300 沪深300)必须使用完整 secid(1.000300)直接存储与使用,
 * 不经过本转换(6位数字对 000xxx 一律按深市股票处理)。
 */
public final class SecCodeConverter {

    private static final Pattern FULL_CODE = Pattern.compile("^(SH|SZ|sh|sz)(\\d{6})$");

    private SecCodeConverter() {
    }

    /**
     * 根据代码推断 secid 前缀(1=沪, 0=深)。
     */
    public static String marketPrefix(String code) {
        if (code == null || code.length() != 6) {
            return "1";
        }
        if (code.startsWith("6") || code.startsWith("5") || code.startsWith("9")
                || code.startsWith("110") || code.startsWith("111")
                || code.startsWith("113") || code.startsWith("118")) {
            return "1";
        }
        return "0";
    }

    /**
     * 校验基金是否为交易所场内交易型基金 (如 ETF / LOF / 封闭式基金 / REITs)。
     * 区分场内与场外的核心规则:
     * 1. 凡是名称包含"联接"或"feeder"的，不论代码是什么，100% 属于场外申赎基金，绝不是场内连续竞价标的。
     * 2. 属于 A 股交易所场内基金专属号段:
     *    - 沪市场内: 50xxxx, 51xxxx, 52xxxx, 56xxxx, 58xxxx (涵盖各类场内ETF/LOF/REITs)
     *    - 深市场内: 159xxx (深市ETF), 16xxxx (深市LOF), 18xxxx (深市封闭式/REITs)
     * 3. 凡不属于上述号段(如 00xxxx, 01xxxx, 02xxxx, 11xxxx 等)的所有基金，一律归为场外基金，按系统算法计算。
     */
    public static boolean isOnMarket(String code, String name) {
        if (code == null || code.length() != 6) {
            return false;
        }
        if (name != null) {
            String lower = name.toLowerCase();
            if (name.contains("联接") || lower.contains("feeder")) {
                return false;
            }
        }
        return code.startsWith("51") || code.startsWith("56") || code.startsWith("58")
                || code.startsWith("50") || code.startsWith("52")
                || code.startsWith("159") || code.startsWith("16") || code.startsWith("18");
    }

    /**
     * 将 6 位代码或带前缀代码(SH600519/SZ000001)转为东财 secid(1.600519 / 0.000001)。
     */
    public static String toSecid(String raw) {
        if (raw == null) {
            return null;
        }
        Matcher m = FULL_CODE.matcher(raw.trim());
        if (m.matches()) {
            String prefix = m.group(1).toUpperCase().startsWith("SH") ? "1" : "0";
            return prefix + "." + m.group(2);
        }
        String code = raw.trim();
        return marketPrefix(code) + "." + code;
    }
}
