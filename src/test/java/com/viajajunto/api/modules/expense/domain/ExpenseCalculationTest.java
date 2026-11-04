package com.viajajunto.api.modules.expense.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;


class ExpenseCalculationTest {

    @Test
    @DisplayName("Deve validar a regra PERCENTAGE lançando exceção se a soma diferir de 100%")
    void shouldValidatePercentageSplitType() {
        // Regra inválida: soma dá 90%
        Expense invalidExpense = new Expense(
                "Jantar",
                new BigDecimal("100.00"),
                "BRL",
                "usr_01",
                SplitType.PERCENTAGE,
                List.of(
                        new ParticipantShare("usr_01", new BigDecimal("50.00")),
                        new ParticipantShare("usr_02", new BigDecimal("40.00"))
                )
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                invalidExpense::validateSplitRules
        );
        assertTrue(ex.getMessage().contains("100"));
    }

    @Test
    @DisplayName("Deve calcular o saldo líquido e sugerir a transferência mínima entre credores e devedores")
    void shouldCalculateNetBalancesAndTransfersCorrectly() {
        // Usuário 1 pagou 300, divisão igualitária para 2 pessoas (150 cada)
        // Usuário 1 deve receber 150 net; Usuário 2 deve pagar 150 net
        List<Expense> expenses = List.of(
                new Expense(
                        "Aluguel",
                        new BigDecimal("300.00"),
                        "BRL",
                        "usr_01",
                        SplitType.EQUAL,
                        List.of(
                                new ParticipantShare("usr_01", null),
                                new ParticipantShare("usr_02", null)
                        )
                )
        );

        BalanceResult result = BalanceCalculator.calculate(expenses, List.of("usr_01", "usr_02"));

        assertEquals(0, new BigDecimal("150.00").compareTo(result.getBalance("usr_01")));
        assertEquals(0, new BigDecimal("-150.00").compareTo(result.getBalance("usr_02")));

        assertEquals(1, result.getTransfers().size());
        Transfer t = result.getTransfers().get(0);
        assertEquals("usr_02", t.getFrom());
        assertEquals("usr_01", t.getTo());
        assertEquals(0, new BigDecimal("150.00").compareTo(t.getAmount()));
    }
}
