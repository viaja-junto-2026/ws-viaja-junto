package com.viajajunto.api.modules.expense.domain;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BalanceResult {

    private final Map<String, BigDecimal> balances;
    private final List<Transfer> transfers;

    public BalanceResult(Map<String, BigDecimal> balances, List<Transfer> transfers) {
        this.balances = balances != null ? Collections.unmodifiableMap(balances) : Collections.emptyMap();
        this.transfers = transfers != null ? Collections.unmodifiableList(transfers) : Collections.emptyList();
    }

    public BigDecimal getBalance(String participantId) {
        return balances.getOrDefault(participantId, BigDecimal.ZERO);
    }

    public Map<String, BigDecimal> getBalances() {
        return balances;
    }

    public List<Transfer> getTransfers() {
        return transfers;
    }

    public List<Balance> toBalanceList() {
        return balances.entrySet().stream()
                .map(entry -> new Balance(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }
}
