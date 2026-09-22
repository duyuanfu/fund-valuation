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
    public Map<String, String> register(@RequestBody Credentials req) {
        authService.register(req.username(), req.password());
        String token = authService.login(req.username(), req.password());
        return Map.of("token", token, "username", req.username());
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Credentials req) {
        String token = authService.login(req.username(), req.password());
        return Map.of("token", token, "username", req.username());
    }

    public record Credentials(String username, String password) {
    }
}
