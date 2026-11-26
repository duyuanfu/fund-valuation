package com.fund.valuation.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fund.valuation.dto.AiDiagnosisReport;
import com.fund.valuation.dto.DiagnosisRiskTag;
import com.fund.valuation.dto.PortfolioLookThroughView;
import com.fund.valuation.dto.StockExposureItem;
import com.fund.valuation.dto.VisionConfigView;
import com.fund.valuation.service.SysVisionConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiAdvisorClient {

    private final SysVisionConfigService visionConfigService;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    private static final String SYSTEM_PROMPT = """
            你是一位拥有20年资产配置与基金投资经验的资深买方投资顾问（CFA持证人）。
            请基于用户基金组合的真实穿透数据、健康度评分和量化风险标签，客观、专业、有深度地输出一份智能持仓诊断体检报告。
            
            输出必须严格为符合以下结构的合法 JSON 对象，且不得输出任何外部 Markdown 标记或代码块：
            {
              "summary": "150字以内的组合诊断综述，总览组合配置特征、风险偏好与核心结论",
              "strengths": ["组合核心亮点1", "亮点2"],
              "weaknesses": ["核心隐患或潜在风险1", "隐患2"],
              "suggestions": ["具体可落地的调仓或优化建议1", "建议2", "建议3"]
            }
            """;

    public AiDiagnosisReport generateReport(
            int healthScore,
            String scoreLevel,
            PortfolioLookThroughView lookThrough,
            List<DiagnosisRiskTag> riskTags
    ) {
        VisionConfigView config = visionConfigService.getConfig();
        if (!Boolean.TRUE.equals(config.enabled()) || config.apiKey() == null || config.apiKey().isBlank()) {
            return fallbackReport(healthScore, scoreLevel, riskTags, "未配置 AI 投顾密钥，已启用本地规则诊断报告。");
        }

        try {
            StringBuilder userPrompt = new StringBuilder();
            userPrompt.append("用户持仓概况：\n");
            userPrompt.append(String.format("- 持仓基金数：%d 只，总金额：%s 元\n", lookThrough.totalFundCount(), lookThrough.totalPortfolioAmount()));
            userPrompt.append(String.format("- 综合健康分：%d 分（评级：%s）\n", healthScore, scoreLevel));

            userPrompt.append("- 资产大类配置：\n");
            lookThrough.assetAllocations().forEach(a ->
                    userPrompt.append(String.format("  * %s: %s 元 (%s%%)\n", a.assetTypeLabel(), a.holdingAmount(), a.ratio()))
            );

            userPrompt.append("- 穿透重仓股票 Top 5：\n");
            int count = 0;
            for (StockExposureItem item : lookThrough.topStockExposures()) {
                if (count++ >= 5) break;
                userPrompt.append(String.format("  * %s(%s): 穿透占比 %s%%\n", item.stockName(), item.stockCode(), item.portfolioRatio()));
            }

            userPrompt.append("- 检测到的风险特征：\n");
            riskTags.forEach(t -> userPrompt.append(String.format("  * [%s] %s: %s\n", t.type(), t.title(), t.description())));

            userPrompt.append("\n请给出深度专业诊断，返回符合要求的纯 JSON。");

            String endpoint = buildChatEndpoint(config.apiBaseUrl());
            String model = (config.modelName() != null && !config.modelName().isBlank()) ? config.modelName() : "glm-4v-flash";

            Map<String, Object> payload = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", userPrompt.toString())
                    ),
                    "temperature", 0.3,
                    "max_tokens", 800
            );

            String requestJson = objectMapper.writeValueAsString(payload);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.apiKey().trim())
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(resp.body());
                JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
                if (contentNode.isTextual()) {
                    String raw = cleanMarkdownJson(contentNode.asText());
                    JsonNode reportNode = objectMapper.readTree(raw);

                    String summary = reportNode.path("summary").asText("");
                    List<String> strengths = readList(reportNode.path("strengths"));
                    List<String> weaknesses = readList(reportNode.path("weaknesses"));
                    List<String> suggestions = readList(reportNode.path("suggestions"));

                    return new AiDiagnosisReport(
                            summary,
                            strengths,
                            weaknesses,
                            suggestions,
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    );
                }
            }
            log.warn("AI 投顾接口调用响应异常: status={}, body={}", resp.statusCode(), resp.body());
        } catch (Exception e) {
            log.warn("AI 投顾生成失败，自动回退到内置规则报告: {}", e.getMessage());
        }

        return fallbackReport(healthScore, scoreLevel, riskTags, "已结合组合量化体检生成诊断综述。");
    }

    private AiDiagnosisReport fallbackReport(
            int healthScore,
            String scoreLevel,
            List<DiagnosisRiskTag> riskTags,
            String note
    ) {
        String summary = String.format("当前基金投资组合综合健康分为 %d 分（评级为【%s】）。%s", healthScore, scoreLevel, note);
        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        for (DiagnosisRiskTag tag : riskTags) {
            if ("SUCCESS".equalsIgnoreCase(tag.type())) {
                strengths.add(tag.title() + "：" + tag.description());
            } else if ("DANGER".equalsIgnoreCase(tag.type()) || "WARNING".equalsIgnoreCase(tag.type())) {
                weaknesses.add(tag.title() + "：" + tag.description());
            }
        }

        if (strengths.isEmpty()) {
            strengths.add("持仓具备基础流动性，底仓资产估值核算完备。");
        }
        if (weaknesses.isEmpty()) {
            weaknesses.add("整体持仓未见显著结构性失衡风险，表现平稳。");
        }

        if (healthScore >= 80) {
            suggestions.add("组合各维度表现均衡，建议保持当前定投或持有节奏，定期季度复盘。");
        } else if (healthScore >= 60) {
            suggestions.add("存在部分行业或重仓股偏离，建议关注重合度较高的同质化基金，适度平衡股债配置。");
            suggestions.add("对集中度过高的单一赛道可在反弹时分批落袋，增配宽基或稳健固收以平抑波动。");
        } else {
            suggestions.add("组合存在较明显的集中暴露风险，强烈建议精简持仓重合度过高的基金产品。");
            suggestions.add("增加低相关性资产（如纯债或货币工具），避免单一市场行情剧烈回撤造成净值大幅波动。");
        }

        return new AiDiagnosisReport(
                summary,
                strengths,
                weaknesses,
                suggestions,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
    }

    private List<String> readList(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                if (item.isTextual() && !item.asText().isBlank()) {
                    result.add(item.asText());
                }
            }
        }
        return result;
    }

    private String cleanMarkdownJson(String raw) {
        if (raw == null) return "{}";
        String s = raw.trim();
        if (s.startsWith("```json")) {
            s = s.substring(7);
        } else if (s.startsWith("```")) {
            s = s.substring(3);
        }
        if (s.endsWith("```")) {
            s = s.substring(0, s.length() - 3);
        }
        return s.trim();
    }

    private String buildChatEndpoint(String apiBaseUrl) {
        String base = (apiBaseUrl != null && !apiBaseUrl.isBlank())
                ? apiBaseUrl.trim()
                : "https://open.bigmodel.cn/api/paas/v4";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith("/chat/completions")) {
            return base;
        }
        return base + "/chat/completions";
    }
}
