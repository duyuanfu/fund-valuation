package com.fund.valuation.web;

import com.fund.valuation.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public RegisterResponse register(@RequestBody RegisterRequest req) {
        authService.register(req.username(), req.password(), req.referralSource());
        return new RegisterResponse("PENDING", req.username(), "注册申请已提交成功！新账号需经管理员审核授权后方可登录，请等待管理员开通。");
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody Credentials req) {
        AuthService.LoginResult res = authService.login(req.username(), req.password());
        return new LoginResponse(res.token(), res.username(), res.role(), res.isVip());
    }

    public record Credentials(String username, String password) {
    }

    public record RegisterRequest(String username, String password, String referralSource) {
    }

    public record LoginResponse(String token, String username, String role, boolean isVip) {
    }

    public record RegisterResponse(String status, String username, String message) {
    }
}
