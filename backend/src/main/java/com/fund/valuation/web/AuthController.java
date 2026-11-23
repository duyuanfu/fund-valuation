package com.fund.valuation.web;

import com.fund.valuation.config.JwtAuthFilter;
import com.fund.valuation.domain.User;
import com.fund.valuation.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public RegisterResponse register(@RequestBody RegisterRequest req) {
        AuthService.RegisterResult res = authService.register(req.username(), req.password(), req.referralSource());
        return new RegisterResponse(
                res.status(),
                res.username(),
                res.message(),
                res.needApproval(),
                res.token(),
                res.role(),
                res.isVip()
        );
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody Credentials req) {
        AuthService.LoginResult res = authService.login(req.username(), req.password());
        return new LoginResponse(res.token(), res.username(), res.role(), res.isVip());
    }

    /**
     * 获取当前登录用户的最新信息与VIP有效状态 (解决客户端无需重新登录即可同步VIP状态)。
     */
    @GetMapping("/me")
    public UserProfileResponse getMe(HttpServletRequest request) {
        String username = (String) request.getAttribute(JwtAuthFilter.ATTR_USERNAME);
        if (username == null) {
            throw new SecurityException("未登录");
        }
        User user = authService.getUser(username);
        if (user == null) {
            throw new SecurityException("用户不存在");
        }
        return new UserProfileResponse(
                user.getUsername(),
                user.getRole(),
                user.getStatus(),
                user.isVipEffective(),
                user.getVipExpireAt()
        );
    }

    public record UserProfileResponse(
            String username,
            String role,
            String status,
            boolean isVip,
            LocalDateTime vipExpireAt
    ) {
    }

    public record Credentials(String username, String password) {
    }

    public record RegisterRequest(String username, String password, String referralSource) {
    }

    public record LoginResponse(String token, String username, String role, boolean isVip) {
    }

    public record RegisterResponse(
            String status,
            String username,
            String message,
            boolean needApproval,
            String token,
            String role,
            boolean isVip
    ) {
    }
}
