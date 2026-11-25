package com.fund.valuation.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fund.valuation.dto.OcrFundItem;
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
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class VisionOcrClient {

    private final SysVisionConfigService visionConfigService;
    private final ObjectMapper objectMapper;

    private static final String PROMPT_SYSTEM = """
            你是一个专业的金融账单与持仓图像结构化解析专家。
            请从用户上传的支付宝基金持仓截图（包含持仓列表或单只基金详情页）中精准提取基金持仓信息，并仅以纯 JSON 数组返回。
            
            每个对象必须严格包含以下字段：
            - fundName: 基金名称（字符串，去除末尾省略号等杂质，例如"广发稳健增长A"）
            - fundCode: 必须填 null（注意：支付宝持仓截图只展示名称，不印6位数字代码，切勿猜测或伪造任何数字代码，一律输出 null）
            - holdingAmount: 持有金额/资产金额（数字，必须大于0，如 50201.26）
            - yesterdayIncome: 昨日收益（数字，可正可负或0，如 -450.34 或 73.12）
            - holdingProfit: 持有收益/累计收益（数字，可正可负或0，如 -1048.74）
            - holdingProfitRate: 持有收益率/累计收益率（百分比数值，例如 -2.05% 输出 -2.05，+1.33% 输出 1.33）
            
            重要规则：
            1. 请务必注意数字的正负符号（例如红色负号、绿色正号或减号 -）；
            2. 如果图片不是基金持仓页面或未识别到任何基金，请输出空的 JSON 数组 []；
            3. 绝不要输出任何解释说明、Markdown 标记或代码块包裹，仅输出标准 JSON 数组文本。
            """;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /**
     * 调用多模态视觉大模型解析持仓截图
     */
    public List<OcrFundItem> extractHoldings(byte[] imageBytes, String mimeType) {
        VisionConfigView config = visionConfigService.getConfig();
        if (!Boolean.TRUE.equals(config.enabled())) {
            throw new IllegalStateException("AI 视觉识别功能暂未启用，请联系管理员开启");
        }
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            throw new IllegalStateException("未配置视觉大模型 API 密钥，请联系管理员配置");
        }

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String resolvedMimeType = (mimeType != null && !mimeType.isBlank()) ? mimeType : "image/jpeg";

        return sendVisionRequest(config, base64Image, resolvedMimeType);
    }

    /**
     * 测试视觉模型接口连通性
     */
    public String testConnection(VisionConfigView config) {
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            throw new IllegalArgumentException("API Key 不能为空");
        }
        String endpoint = buildChatEndpoint(config.apiBaseUrl());
        try {
            Map<String, Object> payload = Map.of(
                    "model", config.modelName() != null ? config.modelName() : "glm-4v-flash",
                    "messages", List.of(
                            Map.of("role", "user", "content", "Ping! Reply with 'PONG' only.")
                    ),
                    "max_tokens", 10
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.apiKey().trim())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return "连接测试失败 (HTTP " + response.statusCode() + "): " + response.body();
            }
            return "连通性测试成功！响应 HTTP 200";
        } catch (Exception e) {
            return "连通性测试异常: " + e.getMessage();
        }
    }

    private List<OcrFundItem> sendVisionRequest(VisionConfigView config, String base64Image, String mimeType) {
        String endpoint = buildChatEndpoint(config.apiBaseUrl());

        Map<String, Object> imageUrlObj = Map.of(
                "url", "data:" + mimeType + ";base64," + base64Image
        );

        List<Map<String, Object>> userContent = List.of(
                Map.of("type", "text", "text", "请提取图片中所有的基金持仓数据，以 JSON 格式输出。"),
                Map.of("type", "image_url", "image_url", imageUrlObj)
        );

        Map<String, Object> payload = Map.of(
                "model", config.modelName() != null ? config.modelName() : "glm-4v-flash",
                "messages", List.of(
                        Map.of("role", "system", "content", PROMPT_SYSTEM),
                        Map.of("role", "user", "content", userContent)
                ),
                "temperature", 0.1
        );

        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.apiKey().trim())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Vision API error HTTP {}: {}", response.statusCode(), response.body());
                throw new IllegalStateException("视觉识别接口调用失败 (HTTP " + response.statusCode() + "): " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.path("choices");
            if (choices.isMissingNode() || !choices.isArray() || choices.isEmpty()) {
                throw new IllegalStateException("视觉模型返回格式异常: 无 choices 结果");
            }

            String content = choices.get(0).path("message").path("content").asText("");
            return parseOcrJson(content);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to call vision OCR: {}", e.getMessage(), e);
            throw new IllegalStateException("视觉识别调用异常: " + e.getMessage(), e);
        }
    }

    public List<OcrFundItem> parseOcrJson(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        String clean = content.trim();
        if (clean.startsWith("```json")) {
            clean = clean.substring(7);
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3);
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length() - 3);
        }
        clean = clean.trim();

        try {
            return objectMapper.readValue(clean, new TypeReference<List<OcrFundItem>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse OCR response as JSON: {}, content: {}", e.getMessage(), clean);
            throw new IllegalStateException("视觉模型返回的数据无法解析为持仓列表: " + e.getMessage());
        }
    }

    private String buildChatEndpoint(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://open.bigmodel.cn/api/paas/v4";
        }
        baseUrl = baseUrl.trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        if (baseUrl.endsWith("/chat/completions")) {
            return baseUrl;
        }
        return baseUrl + "/chat/completions";
    }
}
