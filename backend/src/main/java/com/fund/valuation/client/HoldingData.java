package com.fund.valuation.client;

import java.util.List;

public record HoldingData(String reportDate, List<HoldingRow> rows) {
}