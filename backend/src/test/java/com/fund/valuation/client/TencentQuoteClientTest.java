package com.fund.valuation.client;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TencentQuoteClientTest {

    private final HttpSupport http = new HttpSupport();
    private final TencentQuoteClient client = new TencentQuoteClient(http);

    @Test
    void testBatchQuote() {
        // 请求茅台(1.600519)和美的(0.000333)
        Map<String, Quote> quotes = client.batchQuote(List.of("1.600519", "0.000333"));
        assertNotNull(quotes);
        assertFalse(quotes.isEmpty(), "Tencent quotes should not be empty");

        Quote maotai = quotes.get("1.600519");
        assertNotNull(maotai);
        assertTrue(maotai.name().contains("茅台"));
        assertNotNull(maotai.price());
        assertNotNull(maotai.pctChg());
    }
}
