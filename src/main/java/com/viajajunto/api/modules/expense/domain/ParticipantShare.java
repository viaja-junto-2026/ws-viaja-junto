package com.viajajunto.api.modules.expense.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ParticipantShare {
    private final String participantId;
    private final BigDecimal value;
}
