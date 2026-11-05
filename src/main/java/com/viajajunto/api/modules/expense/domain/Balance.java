package com.viajajunto.api.modules.expense.domain;

import java.math.BigDecimal;

public class Balance {

    private final String participantId;
    private final BigDecimal amount;

    public Balance(String participantId, BigDecimal amount) {
        this.participantId = participantId;
        this.amount = amount;
    }

    public String getParticipantId() {
        return participantId;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}

