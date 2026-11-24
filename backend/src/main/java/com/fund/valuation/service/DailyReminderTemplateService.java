package com.fund.valuation.service;

import com.fund.valuation.dto.PortfolioSummaryView;
import com.fund.valuation.dto.PositionItemView;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class DailyReminderTemplateService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy年MM月dd日");

    /**
     * 渲染高信息密度且适配移动端的 HTML 富文本邮件
     */
    public String renderHtmlEmail(String username,
                                  PortfolioSummaryView portfolioSummary,
                                  List<PositionItemView> positions,
                                  List<EstimateResult> watchlist,
                                  boolean hideAmount,
                                  String websiteUrl) {
        String todayStr = LocalDate.now().format(DATE_FMT);
        StringBuilder sb = new StringBuilder();

        sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'/><meta name='viewport' content='width=device-width, initial-scale=1.0'/></head>");
        sb.append("<body style='margin:0;padding:16px;background:#f8fafc;font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,sans-serif;'>");
        sb.append("<div style='max-width:620px;margin:0 auto;background:#ffffff;border-radius:14px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,0.05);border:1px solid #e2e8f0;'>");

        // 头部 Banner
        sb.append("<div style='background:linear-gradient(135deg, #1e293b 0%, #0f172a 100%);padding:22px 24px;color:#ffffff;'>");
        sb.append("<div style='font-size:12px;color:#94a3b8;font-weight:600;letter-spacing:1px;text-transform:uppercase;'>收盘前30分钟 · 调仓黄金决策窗口</div>");
        sb.append("<div style='font-size:20px;font-weight:700;margin-top:6px;display:flex;align-items:center;'>📈 14:30 盘中估值与持仓日报</div>");
        sb.append("<div style='font-size:13px;color:#cbd5e1;margin-top:4px;'>").append(todayStr).append(" · 用户: ").append(username).append("</div>");
        sb.append("</div>");

        sb.append("<div style='padding:20px 24px;'>");

        // 1. 若有持仓数据，渲染持仓收益总览卡片
        if (portfolioSummary != null && portfolioSummary.totalMarketValue() != null) {
            BigDecimal todayIncome = portfolioSummary.totalTodayIncome() != null ? portfolioSummary.totalTodayIncome() : BigDecimal.ZERO;
            BigDecimal incomePct = portfolioSummary.totalTodayIncomePct() != null ? portfolioSummary.totalTodayIncomePct() : BigDecimal.ZERO;
            boolean isPositive = todayIncome.compareTo(BigDecimal.ZERO) >= 0;
            String color = isPositive ? "#dc2626" : "#16a34a";
            String sign = isPositive ? "+" : "";

            sb.append("<div style='background:#f8fafc;border-radius:10px;padding:16px;margin-bottom:20px;border:1px solid #e2e8f0;'>");
            sb.append("<div style='font-size:13px;color:#64748b;font-weight:600;'>今日持仓预估盈亏</div>");
            sb.append("<div style='display:flex;align-items:baseline;gap:8px;margin-top:6px;'>");

            if (!hideAmount) {
                sb.append("<span style='font-size:26px;font-weight:800;color:").append(color).append(";'>")
                        .append(sign).append(String.format("%.2f", todayIncome.doubleValue())).append("元</span>");
            }
            sb.append("<span style='font-size:18px;font-weight:700;color:").append(color).append(";'>")
                    .append(sign).append(String.format("%.2f%%", incomePct.doubleValue())).append("</span>");

            sb.append("</div>");

            if (!hideAmount) {
                sb.append("<div style='margin-top:8px;font-size:12px;color:#64748b;'>总资产现值: ¥")
                        .append(String.format("%.2f", portfolioSummary.totalMarketValue().doubleValue())).append("</div>");
            }
            sb.append("</div>");
        }

        // 2. 自选基金估值列表
        if (watchlist != null && !watchlist.isEmpty()) {
            sb.append("<div style='font-size:15px;font-weight:700;color:#0f172a;margin-bottom:10px;'>自选基金盘中估算</div>");
            sb.append("<table style='width:100%;border-collapse:collapse;font-size:13px;text-align:left;'>");
            sb.append("<tr style='background:#f1f5f9;color:#475569;'>");
            sb.append("<th style='padding:8px 10px;border-radius:6px 0 0 6px;'>基金名称/代码</th>");
            sb.append("<th style='padding:8px 10px;text-align:right;'>估算净值</th>");
            sb.append("<th style='padding:8px 10px;text-align:right;border-radius:0 6px 6px 0;'>盘中涨跌幅</th>");
            sb.append("</tr>");

            for (EstimateResult item : watchlist) {
                BigDecimal pct = item.estimatePct() != null ? item.estimatePct() : BigDecimal.ZERO;
                boolean isUp = pct.compareTo(BigDecimal.ZERO) >= 0;
                String color = isUp ? "#dc2626" : "#16a34a";
                String sign = isUp ? "+" : "";

                sb.append("<tr style='border-bottom:1px solid #f1f5f9;'>");
                sb.append("<td style='padding:10px;'>");
                sb.append("<div style='font-weight:600;color:#1e293b;'>").append(item.fundName()).append("</div>");
                sb.append("<div style='font-size:11px;color:#94a3b8;'>").append(item.fundCode()).append("</div>");
                sb.append("</td>");
                sb.append("<td style='padding:10px;text-align:right;font-weight:600;color:#334155;'>")
                        .append(item.estimateNav() != null ? String.format("%.4f", item.estimateNav().doubleValue()) : "-")
                        .append("</td>");
                sb.append("<td style='padding:10px;text-align:right;'>");
                sb.append("<span style='display:inline-block;padding:2px 8px;border-radius:4px;font-weight:700;color:").append(color)
                        .append(";background:").append(isUp ? "#fee2e2" : "#dcfce7").append(";'>")
                        .append(sign).append(String.format("%.2f%%", pct.doubleValue()))
                        .append("</span>");
                sb.append("</td>");
                sb.append("</tr>");
            }
            sb.append("</table>");
        }

        // 3. 提示与按钮
        sb.append("<div style='margin-top:24px;text-align:center;'>");
        sb.append("<a href='").append(websiteUrl != null ? websiteUrl : "http://localhost:8080")
                .append("' style='display:inline-block;padding:10px 24px;background:#2563eb;color:#ffffff;text-decoration:none;border-radius:8px;font-weight:600;font-size:14px;'>点击直达自选与持仓看板 ➔</a>");
        sb.append("</div>");

        sb.append("<div style='margin-top:20px;font-size:11px;color:#94a3b8;line-height:1.6;border-top:1px solid #f1f5f9;padding-top:12px;'>");
        sb.append("📌 风险提示：盘中估算基于重仓股与成分走势模拟测算，仅供盘中决策参考，不作为最终确认净值。若需调仓或止盈，请留意场外基金 15:00 申赎截止时点。");
        sb.append("</div>");

        sb.append("</div></div></body></html>");
        return sb.toString();
    }

    /**
     * 渲染微信与 Webhook 推送的 Markdown 消息
     */
    public String renderMarkdown(String username,
                                 PortfolioSummaryView portfolioSummary,
                                 List<PositionItemView> positions,
                                 List<EstimateResult> watchlist,
                                 boolean hideAmount) {
        String todayStr = LocalDate.now().format(DATE_FMT);
        StringBuilder sb = new StringBuilder();

        sb.append("### 📈 14:30 盘中估值与持仓日报\n\n");
        sb.append("> **日期**: ").append(todayStr).append(" (收盘前半小时决策窗口)\n\n");

        if (portfolioSummary != null && portfolioSummary.totalMarketValue() != null) {
            BigDecimal todayIncome = portfolioSummary.totalTodayIncome() != null ? portfolioSummary.totalTodayIncome() : BigDecimal.ZERO;
            BigDecimal incomePct = portfolioSummary.totalTodayIncomePct() != null ? portfolioSummary.totalTodayIncomePct() : BigDecimal.ZERO;
            String sign = todayIncome.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";

            sb.append("#### 💰 今日持仓预估\n");
            if (!hideAmount) {
                sb.append("- **今日预估盈亏**: `").append(sign).append(String.format("%.2f元", todayIncome.doubleValue())).append("` (")
                        .append(sign).append(String.format("%.2f%%", incomePct.doubleValue())).append(")\n");
                sb.append("- **持仓资产总值**: ¥").append(String.format("%.2f", portfolioSummary.totalMarketValue().doubleValue())).append("\n\n");
            } else {
                sb.append("- **今日预估涨跌**: `").append(sign).append(String.format("%.2f%%", incomePct.doubleValue())).append("`\n\n");
            }
        }

        if (watchlist != null && !watchlist.isEmpty()) {
            sb.append("#### 📋 重点自选盘中走势\n");
            int count = 0;
            for (EstimateResult item : watchlist) {
                BigDecimal pct = item.estimatePct() != null ? item.estimatePct() : BigDecimal.ZERO;
                String sign = pct.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
                sb.append("- **").append(item.fundName()).append("** (`").append(item.fundCode()).append("`): ")
                        .append(item.estimateNav() != null ? String.format("%.4f", item.estimateNav().doubleValue()) : "-")
                        .append(" (").append(sign).append(String.format("%.2f%%", pct.doubleValue())).append(")\n");
                count++;
                if (count >= 8) { // 限制前 8 条避免消息过长
                    sb.append("- ...(更多请前往网页查看)\n");
                    break;
                }
            }
            sb.append("\n");
        }

        sb.append("> ⚠️ 提示：盘中模拟估算仅供参考，15:00 前申赎可按今日收盘净值确认。");
        return sb.toString();
    }
}
