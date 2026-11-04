package com.viajajunto.api.modules.expense.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class BalanceResult {
    private final Map<String, BigDecimal> balances;
    private final List<Transfer> transfers;

    public BalanceResult(Map<String, BigDecimal> balances, List<Transfer> transfers) {
        this.balances = balances;
        this.transfers = transfers;
    }

    public BigDecimal getBalance(String participantId) {
        return balances.getOrDefault(participantId, BigDecimal.ZERO);
    }

    public List<Transfer> getTransfers() {
        return transfers;
    }
}
