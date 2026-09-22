package com.fund.valuation.service;

import com.fund.valuation.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 签发与解析(HS256,subject=username)。
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expireSeconds;

    public JwtService(AppProperties properties) {
        String secret = properties.getJwt().getSecret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("jwt.secret 未配置或过短(至少32字符)");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireSeconds = properties.getJwt().getExpireDays() * 86400L;
    }

    public String generate(String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireSeconds)))
                .signWith(key)
                .compact();
    }

    /**
     * 解析并返回 username,无效/过期返回 null。
     */
    public String parseUsername(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
