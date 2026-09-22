package com.fund.valuation.service;

import com.fund.valuation.config.AppProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private final AppProperties props = new AppProperties();

    @Test
    void generateAndParse() {
        JwtService jwt = new JwtService(props);
        String token = jwt.generate("u1");
        assertNotNull(token);
        assertEquals("u1", jwt.parseUsername(token));
    }

    @Test
    void invalidTokenReturnsNull() {
        JwtService jwt = new JwtService(props);
        assertNull(jwt.parseUsername("bad.token.value"));
        assertNull(jwt.parseUsername(null));
    }

    @Test
    void shortSecretRejected() {
        AppProperties p = new AppProperties();
        p.getJwt().setSecret("short");
        assertThrows(IllegalStateException.class, () -> new JwtService(p));
    }

    @Test
    void tokensForDifferentUsersDiffer() {
        JwtService jwt = new JwtService(props);
        assertNotEquals(jwt.generate("a"), jwt.generate("b"));
    }
}
