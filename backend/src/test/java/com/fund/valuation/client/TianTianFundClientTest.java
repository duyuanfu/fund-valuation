package com.fund.valuation.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fund.valuation.dto.NavPointView;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TianTianFundClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TianTianFundClient client = new TianTianFundClient(new HttpSupport(), objectMapper);

    @Test
    void testParsePerformanceJson() {
        String json = """
                {
                    "Datas": [
                        { "FSRQ": "2026-10-20", "DWJZ": "2.000", "LJJZ": "2.000", "JZZZL": "0.00" },
                        { "FSRQ": "2026-10-21", "DWJZ": "2.020", "LJJZ": "2.020", "JZZZL": "1.00" },
                        { "FSRQ": "2026-10-22", "DWJZ": "2.000", "LJJZ": "2.000", "JZZZL": "-0.99" }
                    ],
                    "ErrCode": 0
                }
                """;

        List<NavPointView> points = client.parsePerformanceJson(json);
        assertNotNull(points);
        assertEquals(3, points.size());

        NavPointView p0 = points.get(0);
        assertEquals("2026-10-20", p0.date());
        assertEquals(new BigDecimal("2.000"), p0.nav());
        assertEquals(new BigDecimal("0.00"), p0.dailyGrowthPct());
        assertEquals(new BigDecimal("0.00"), p0.accumReturnPct());

        NavPointView p1 = points.get(1);
        assertEquals("2026-10-21", p1.date());
        assertEquals(new BigDecimal("2.020"), p1.nav());
        assertEquals(new BigDecimal("1.00"), p1.dailyGrowthPct());
        assertEquals(new BigDecimal("1.00"), p1.accumReturnPct());

        NavPointView p2 = points.get(2);
        assertEquals("2026-10-22", p2.date());
        assertEquals(new BigDecimal("-0.99"), p2.dailyGrowthPct());
        assertNotNull(p2.accumReturnPct());
    }

    @Test
    void testParsePerformanceJson_emptyOrInvalid() {
        assertTrue(client.parsePerformanceJson(null).isEmpty());
        assertTrue(client.parsePerformanceJson("").isEmpty());
        assertTrue(client.parsePerformanceJson("{\"Datas\":[]}").isEmpty());
        assertTrue(client.parsePerformanceJson("invalid json").isEmpty());
    }
}
