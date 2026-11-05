package com.viajajunto.api.modules.expense.domain;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class Expense {

    private final String description;
    private final BigDecimal amount;
    private final String currency;
    private final String payerId;
    private final SplitType type;
    private final List<ParticipantShare> shares;

    public Expense(String description, BigDecimal amount, String currency, String payerId, SplitType type, List<ParticipantShare> shares) {
        this.description = description;
        this.amount = amount;
        this.currency = currency;
        this.payerId = payerId;
        this.type = type;
        this.shares = shares != null ? List.copyOf(shares) : Collections.emptyList();
    }

    public void validateSplitRules() {
        if (type == null) {
            throw new IllegalArgumentException("SplitType cannot be null.");
        }
        if (shares == null || shares.isEmpty()) {
            throw new IllegalArgumentException("Shares cannot be empty.");
        }

        switch (type) {
            case EQUAL -> {
                if (shares.stream().anyMatch(s -> s.getValue() != null)) {
                    throw new IllegalArgumentException("EQUAL split type should not have specific share values.");
                }
            }
            case PERCENTAGE -> {
                if (shares.stream().anyMatch(s -> s.getValue() == null)) {
                    throw new IllegalArgumentException("PERCENTAGE split requires all shares to have values.");
                }
                BigDecimal sum = shares.stream()
                        .map(ParticipantShare::getValue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (sum.compareTo(new BigDecimal("100")) != 0) {
                    throw new IllegalArgumentException("PERCENTAGE split must sum exactly to 100.");
                }
            }
            case FIXED -> {
                if (shares.stream().anyMatch(s -> s.getValue() == null)) {
                    throw new IllegalArgumentException("FIXED split requires all shares to have values.");
                }
                BigDecimal sum = shares.stream()
                        .map(ParticipantShare::getValue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (sum.compareTo(amount) != 0) {
                    throw new IllegalArgumentException("FIXED split values must sum exactly to the expense amount.");
                }
            }
            default -> throw new IllegalStateException("Unsupported split type: " + type);
        }
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getPayerId() {
        return payerId;
    }

    public SplitType getType() {
        return type;
    }

    public List<ParticipantShare> getShares() {
        return shares;
    }
}
