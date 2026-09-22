package com.fund.valuation.web;

import com.fund.valuation.config.JwtAuthFilter;
import com.fund.valuation.domain.UserFund;
import com.fund.valuation.service.EstimateResult;
import com.fund.valuation.service.WatchlistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    /**
     * 自选估值列表(身份来自 JWT)。
     */
    @GetMapping
    public List<EstimateResult> list(HttpServletRequest request) {
        return watchlistService.estimates(currentUser(request));
    }

    /**
     * 添加自选。
     */
    @PostMapping
    public ResponseEntity<?> add(HttpServletRequest request, @Valid @RequestBody AddRequest req) {
        UserFund uf = watchlistService.add(currentUser(request), req.fundCode());
        return ResponseEntity.ok(Map.of("success", true, "fundCode", req.fundCode()));
    }

    /**
     * 删除自选。
     */
    @DeleteMapping("/{fundCode}")
    public ResponseEntity<?> remove(HttpServletRequest request, @PathVariable String fundCode) {
        watchlistService.remove(currentUser(request), fundCode);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * 自选排序。
     */
    @PutMapping
    public ResponseEntity<?> reorder(HttpServletRequest request, @RequestBody ReorderRequest req) {
        watchlistService.reorder(currentUser(request), req.fundCodes());
        return ResponseEntity.ok(Map.of("success", true));
    }

    private String currentUser(HttpServletRequest request) {
        return (String) request.getAttribute(JwtAuthFilter.ATTR_USERNAME);
    }

    public record AddRequest(@NotBlank String fundCode) {
    }

    public record ReorderRequest(List<String> fundCodes) {
    }
}
