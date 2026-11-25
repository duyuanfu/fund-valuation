package com.fund.valuation.service;

import com.fund.valuation.client.TianTianFundClient.FundSearchItem;
import com.fund.valuation.client.VisionOcrClient;
import com.fund.valuation.dto.OcrFundItem;
import com.fund.valuation.dto.ScreenshotImportResult;
import com.fund.valuation.dto.VisionConfigView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioScreenshotService {

    private final VisionOcrClient visionOcrClient;
    private final FundSearchService fundSearchService;
    private final SysVisionConfigService visionConfigService;

    private static final int MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    // 内存中记录每个用户当天的使用次数: "username:2026-11-25" -> 次数
    private final ConcurrentHashMap<String, AtomicInteger> userDailyUsage = new ConcurrentHashMap<>();

    /**
     * 处理持仓截图（严格内存处理，不落盘）
     */
    public ScreenshotImportResult processScreenshot(String userId, byte[] imageBytes, String contentType) {
        if (imageBytes == null || imageBytes.length == 0) {
            return new ScreenshotImportResult(false, "上传图片内容为空", List.of(), 0);
        }

        if (imageBytes.length > MAX_FILE_SIZE) {
            return new ScreenshotImportResult(false, "图片大小超过 5MB 限制，请先压缩或裁剪后上传", List.of(), 0);
        }

        VisionConfigView config = visionConfigService.getConfig();
        if (!Boolean.TRUE.equals(config.enabled())) {
            return new ScreenshotImportResult(false, "系统未启用 AI 识图功能，请联系管理员", List.of(), 0);
        }

        // 每日频次限制检查
        int limit = config.dailyUserLimit() != null && config.dailyUserLimit() > 0 ? config.dailyUserLimit() : 20;
        String usageKey = userId + ":" + LocalDate.now();
        AtomicInteger counter = userDailyUsage.computeIfAbsent(usageKey, k -> new AtomicInteger(0));
        if (counter.get() >= limit) {
            return new ScreenshotImportResult(false, "您今日的识图配额已用完（每日上限 " + limit + " 次），请明日再试", List.of(), 0);
        }

        String mimeType = normalizeMimeType(imageBytes, contentType);

        try {
            // 1. 调用云端多模态大模型抽取结构化 JSON
            List<OcrFundItem> rawItems = visionOcrClient.extractHoldings(imageBytes, mimeType);
            if (rawItems.isEmpty()) {
                return new ScreenshotImportResult(false, "未能从图片中识别到支付宝基金持仓信息，请确保图片清晰且为持仓页面", List.of(), limit - counter.get());
            }

            // 2. 为每只基金自动关联 6 位代码候选 (Top-5)
            // 绝不盲目采纳视觉大模型可能幻觉出的代码，必须通过权威天天基金目录进行检索核验
            List<OcrFundItem> enrichedItems = new ArrayList<>();
            for (OcrFundItem raw : rawItems) {
                List<FundSearchItem> candidates = fundSearchService.search(raw.fundName(), 5);
                String resolvedCode = null;
                // 若模型提取的代码经官方候选名单核验存在，则保留；否则以权威检索排名第一的代码为准
                if (raw.fundCode() != null && candidates.stream().anyMatch(c -> c.code().equals(raw.fundCode()))) {
                    resolvedCode = raw.fundCode();
                } else if (!candidates.isEmpty()) {
                    resolvedCode = candidates.get(0).code();
                }

                String warn = resolvedCode == null ? "未自动匹配到6位基金代码，请手动输入或选择" : null;
                enrichedItems.add(new OcrFundItem(
                        raw.fundName(),
                        resolvedCode,
                        raw.holdingAmount(),
                        raw.yesterdayIncome(),
                        raw.holdingProfit(),
                        raw.holdingProfitRate(),
                        candidates,
                        warn
                ));
            }

            // 扣减当日额度
            int used = counter.incrementAndGet();
            int remaining = Math.max(0, limit - used);

            return new ScreenshotImportResult(
                    true,
                    "成功识别 " + enrichedItems.size() + " 只基金持仓！请核对并确认导入。",
                    enrichedItems,
                    remaining
            );
        } catch (Exception e) {
            log.error("Screenshot import failed for user {}: {}", userId, e.getMessage(), e);
            int remaining = Math.max(0, limit - counter.get());
            return new ScreenshotImportResult(false, e.getMessage() != null ? e.getMessage() : "识别处理失败", List.of(), remaining);
        }
    }

    /**
     * 根据魔数探测图片类型
     */
    private String normalizeMimeType(byte[] bytes, String contentType) {
        if (bytes.length >= 3 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) {
            return "image/jpeg";
        }
        if (bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == (byte) 0x50 && bytes[2] == (byte) 0x4E && bytes[3] == (byte) 0x47) {
            return "image/png";
        }
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        if (contentType != null && !contentType.isBlank() && contentType.startsWith("image/")) {
            return contentType;
        }
        return "image/jpeg";
    }
}
