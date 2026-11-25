package com.fund.valuation.web;

import com.fund.valuation.config.JwtAuthFilter;
import com.fund.valuation.domain.User;
import com.fund.valuation.domain.UserPosition;
import com.fund.valuation.dto.PortfolioView;
import com.fund.valuation.dto.PositionSaveRequest;
import com.fund.valuation.dto.ScreenshotImportResult;
import com.fund.valuation.dto.VipConfigView;
import com.fund.valuation.dto.VipOrderNotifyRequest;
import com.fund.valuation.service.AuthService;
import com.fund.valuation.service.PortfolioScreenshotService;
import com.fund.valuation.service.PortfolioService;
import com.fund.valuation.service.SysVipConfigService;
import com.fund.valuation.service.VipOrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PortfolioScreenshotService portfolioScreenshotService;
    private final AuthService authService;
    private final SysVipConfigService sysVipConfigService;
    private final VipOrderService vipOrderService;

    /**
     * 获取VIP充值与价格配置 (供前台充值弹窗展示，无需VIP权限)。
     */
    @GetMapping("/vip-config")
    public ResponseEntity<VipConfigView> getVipConfig() {
        return ResponseEntity.ok(sysVipConfigService.getVipConfig());
    }

    /**
     * 用户完成支付后提交充值通知，向管理员发送实时推送消息并生成待审批订单。
     */
    @PostMapping("/recharge-notify")
    public ResponseEntity<?> notifyRecharge(HttpServletRequest request, @RequestBody VipOrderNotifyRequest req) {
        String username = currentUser(request);
        if (username == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }
        vipOrderService.submitOrder(username, req);
        return ResponseEntity.ok(Map.of("success", true, "message", "充值申请已成功提交并通知管理员，请耐心等待开通！"));
    }

    /**
     * 获取当前用户的持仓列表与收益预估汇总 (VIP 专享)。
     */
    @GetMapping
    public ResponseEntity<PortfolioView> getPortfolio(HttpServletRequest request) {
        String username = currentUser(request);
        assertVip(username);
        return ResponseEntity.ok(portfolioService.getPortfolio(username));
    }

    /**
     * 新增或编辑持仓记录 (VIP 专享，对标支付宝4字段与智能联动)。
     */
    @PostMapping
    public ResponseEntity<?> save(HttpServletRequest request, @Valid @RequestBody PositionSaveRequest req) {
        String username = currentUser(request);
        assertVip(username);
        UserPosition pos = portfolioService.savePosition(username, req);
        return ResponseEntity.ok(Map.of("success", true, "id", pos.getId(), "fundCode", pos.getFundCode()));
    }

    /**
     * 批量新增或更新持仓记录 (VIP 专享，用于截图识别后一键导入)。
     */
    @PostMapping("/batch")
    public ResponseEntity<?> batchSave(HttpServletRequest request, @Valid @RequestBody List<PositionSaveRequest> items) {
        String username = currentUser(request);
        assertVip(username);
        List<UserPosition> saved = portfolioService.batchSavePositions(username, items);
        return ResponseEntity.ok(Map.of("success", true, "count", saved.size()));
    }

    /**
     * 上传持仓截图进行 AI 结构化识别 (VIP 专享，支持 FormData 文件上传)。
     */
    @PostMapping(value = "/import-screenshot", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importScreenshotMultipart(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file
    ) {
        String username = currentUser(request);
        assertVip(username);
        try {
            byte[] bytes = file.getBytes();
            ScreenshotImportResult result = portfolioScreenshotService.processScreenshot(username, bytes, file.getContentType());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "读取上传图片失败: " + e.getMessage()));
        }
    }

    /**
     * 接收 Base64 图片进行 AI 结构化识别 (VIP 专享，支持剪贴板粘贴与 Canvas 压缩直接上传)。
     */
    @PostMapping(value = "/import-screenshot", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> importScreenshotBase64(
            HttpServletRequest request,
            @RequestBody Map<String, String> body
    ) {
        String username = currentUser(request);
        assertVip(username);

        String base64 = body.get("image");
        if (base64 == null || base64.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "图片内容不能为空"));
        }

        String mimeType = body.getOrDefault("contentType", "image/jpeg");
        if (base64.contains(",")) {
            String[] parts = base64.split(",", 2);
            if (parts[0].contains("image/")) {
                int start = parts[0].indexOf("image/");
                int end = parts[0].indexOf(";");
                if (start >= 0 && end > start) {
                    mimeType = parts[0].substring(start, end);
                }
            }
            base64 = parts[1];
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(base64.trim());
            ScreenshotImportResult result = portfolioScreenshotService.processScreenshot(username, bytes, mimeType);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "图片 Base64 解码失败: " + e.getMessage()));
        }
    }

    /**
     * 删除单只持仓基金 (VIP 专享)。
     */
    @DeleteMapping("/{fundCode}")
    public ResponseEntity<?> remove(HttpServletRequest request, @PathVariable String fundCode) {
        String username = currentUser(request);
        assertVip(username);
        portfolioService.removePosition(username, fundCode);
        return ResponseEntity.ok(Map.of("success", true));
    }

    private void assertVip(String username) {
        if (username == null) {
            throw new SecurityException("请先登录账号");
        }
        User user = authService.getUser(username);
        if (user == null || !user.isVipEffective()) {
            throw new SecurityException("持仓收益实时估算为 VIP 会员专享特权，请开通 VIP 会员后使用");
        }
    }

    private String currentUser(HttpServletRequest request) {
        return (String) request.getAttribute(JwtAuthFilter.ATTR_USERNAME);
    }
}
