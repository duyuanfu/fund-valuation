package com.fund.valuation.service;

import com.fund.valuation.config.AppProperties;
import com.fund.valuation.mapper.BondDurationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
class BondDurationServiceTest {

    @Autowired
    private BondDurationService durationService;

    @Autowired
    private BondDurationMapper bondDurationMapper;

    @BeforeEach
    void cleanDb() {
        bondDurationMapper.delete(null);
    }

    @Test
    void inferByTypeRaw() {
        AppProperties props = new AppProperties();
        BondDurationService service = new BondDurationService(null, props);
        assertEquals(4.5, service.inferByTypeRaw("债券型-长债"));
        assertEquals(2.0, service.inferByTypeRaw("债券型-中短债"));
        assertEquals(1.5, service.inferByTypeRaw("债券型-短债"));
        assertEquals(3.0, service.inferByTypeRaw("债券型-信用债"));
        assertNull(service.inferByTypeRaw("股票型"));
    }

    @Test
    void inferByKeyword() {
        AppProperties props = new AppProperties();
        BondDurationService service = new BondDurationService(null, props);
        assertEquals(1.5, service.inferByKeyword("xx短债A"));
        assertEquals(2.0, service.inferByKeyword("xx中短债债券"));
        assertEquals(4.5, service.inferByKeyword("xx国开行长债"));
        assertEquals(1.5, service.inferByKeyword("xx1-3年国开债"));
        assertEquals(3.0, service.inferByKeyword("xx信用债精选"));
        assertNull(service.inferByKeyword("xx稳健债券"));
    }

    @Test
    void typeRawTakesPriorityOverName() {
        AppProperties props = new AppProperties();
        BondDurationMapper mockMapper = org.mockito.Mockito.mock(BondDurationMapper.class);
        org.mockito.Mockito.when(mockMapper.selectOne(org.mockito.ArgumentMatchers.any()))
                .thenReturn(null);
        BondDurationService service = new BondDurationService(mockMapper, props);
        // 官方类型"长债"优先于名称"短债"
        assertEquals(4.5, service.getDuration("008559", "xx短债债券", "债券型-长债"));
    }

    @Test
    void fallbackToDefault() {
        AppProperties props = new AppProperties();
        BondDurationMapper mockMapper = org.mockito.Mockito.mock(BondDurationMapper.class);
        org.mockito.Mockito.when(mockMapper.selectOne(org.mockito.ArgumentMatchers.any()))
                .thenReturn(null);
        BondDurationService service = new BondDurationService(mockMapper, props);
        assertEquals(2.0, service.getDuration("008559", "xx稳健债券", "股票型"), 0.001);
    }

    @Test
    void upsertAndRead() {
        durationService.upsert("008559", "2026-06-30", 2.5, "report");
        double d = durationService.getDuration("008559", "xx纯债", "债券型-长债");
        assertEquals(2.5, d, 0.001);

        // 报告期覆盖
        durationService.upsert("008559", "2026-03-31", 3.0, "report");
        durationService.upsert("008559", "2026-06-30", 2.6, "report");
        double latest = durationService.getDuration("008559", "xx纯债", "债券型-长债");
        assertEquals(2.6, latest, 0.001);
    }

    @Test
    void tableOverridesTypeRaw() {
        durationService.upsert("008559", "2026-06-30", 3.5, "report");
        assertEquals(3.5, durationService.getDuration("008559", "xx短债", "债券型-长债"), 0.001);
    }
}