package com.fund.valuation.web;

import com.fund.valuation.client.VisionOcrClient;
import com.fund.valuation.dto.VisionConfigView;
import com.fund.valuation.service.SysVisionConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/vision-config")
@RequiredArgsConstructor
public class AdminVisionController {

    private final SysVisionConfigService visionConfigService;
    private final VisionOcrClient visionOcrClient;

    @GetMapping
    public ResponseEntity<VisionConfigView> getConfig() {
        return ResponseEntity.ok(visionConfigService.getMaskedConfig());
    }

    @PostMapping
    public ResponseEntity<VisionConfigView> updateConfig(@RequestBody VisionConfigView req) {
        return ResponseEntity.ok(visionConfigService.updateConfig(req));
    }

    @PostMapping("/test")
    public ResponseEntity<?> testConnection(@RequestBody(required = false) VisionConfigView req) {
        VisionConfigView targetConfig;
        if (req != null && req.apiKey() != null && !req.apiKey().isBlank() && !req.apiKey().contains("****")) {
            targetConfig = req;
        } else {
            targetConfig = visionConfigService.getConfig();
        }

        if (targetConfig.apiKey() == null || targetConfig.apiKey().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "未配置 API Key，无法测试"));
        }

        String result = visionOcrClient.testConnection(targetConfig);
        boolean success = result.contains("成功") || result.contains("HTTP 200");
        return ResponseEntity.ok(Map.of("success", success, "message", result));
    }
}
