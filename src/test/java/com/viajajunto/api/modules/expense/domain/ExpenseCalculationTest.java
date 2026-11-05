package com.viajajunto.api.modules.expense.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpenseCalculationTest {

    @Test
    @DisplayName("Deve validar a regra PERCENTAGE lançando exceção se a soma diferir de 100%")
    void shouldValidatePercentageSplitType() {
        Participant p1 = new Participant("usr_01", "Ana");
        Participant p2 = new Participant("usr_02", "Bruno");

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

    @Test
    @DisplayName("Deve atribuir o resto da divisão EQUAL com dízima ao primeiro participante")
    void shouldAssignRemainderToFirstParticipantInEqualSplit() {
        // R$ 100,00 dividido entre 3 pessoas:
        // 100 / 3 = 33.33 cada, resto de 0.01 vai para o primeiro participante -> 33.34
        List<Expense> expenses = List.of(
                new Expense(
                        "Almoço",
                        new BigDecimal("100.00"),
                        "BRL",
                        "usr_01",
                        SplitType.EQUAL,
                        List.of(
                                new ParticipantShare("usr_01", null),
                                new ParticipantShare("usr_02", null),
                                new ParticipantShare("usr_03", null)
                        )
                )
        );

        BalanceResult result = BalanceCalculator.calculate(expenses, List.of("usr_01", "usr_02", "usr_03"));

        // usr_01 pagou 100.00 e sua parte é 33.34 -> saldo +66.66
        assertEquals(0, new BigDecimal("66.66").compareTo(result.getBalance("usr_01")));
        // usr_02 não pagou e sua parte é 33.33 -> saldo -33.33
        assertEquals(0, new BigDecimal("-33.33").compareTo(result.getBalance("usr_02")));
        // usr_03 não pagou e sua parte é 33.33 -> saldo -33.33
        assertEquals(0, new BigDecimal("-33.33").compareTo(result.getBalance("usr_03")));

        // Soma das transferências deve quitar as dívidas
        BigDecimal totalTransferred = result.getTransfers().stream()
                .map(Transfer::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("66.66").compareTo(totalTransferred));
    }

    @Test
    @DisplayName("Deve validar a regra FIXED lançando exceção se a soma diferir do total da despesa")
    void shouldValidateFixedSplitType() {
        Expense invalidExpense = new Expense(
                "Supermercado",
                new BigDecimal("100.00"),
                "BRL",
                "usr_01",
                SplitType.FIXED,
                List.of(
                        new ParticipantShare("usr_01", new BigDecimal("50.00")),
                        new ParticipantShare("usr_02", new BigDecimal("40.00"))
                )
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                invalidExpense::validateSplitRules
        );
        assertTrue(ex.getMessage().contains("FIXED"));

        Expense validExpense = new Expense(
                "Supermercado",
                new BigDecimal("100.00"),
                "BRL",
                "usr_01",
                SplitType.FIXED,
                List.of(
                        new ParticipantShare("usr_01", new BigDecimal("60.00")),
                        new ParticipantShare("usr_02", new BigDecimal("40.00"))
                )
        );
        assertDoesNotThrow(validExpense::validateSplitRules);
    }

    @Test
    @DisplayName("Deve validar a regra EQUAL lançando exceção se houver valor explícito informado")
    void shouldValidateEqualSplitType() {
        Expense invalidExpense = new Expense(
                "Combustível",
                new BigDecimal("100.00"),
                "BRL",
                "usr_01",
                SplitType.EQUAL,
                List.of(
                        new ParticipantShare("usr_01", new BigDecimal("50.00")),
                        new ParticipantShare("usr_02", null)
                )
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                invalidExpense::validateSplitRules
        );
        assertTrue(ex.getMessage().contains("EQUAL"));

        Expense validExpense = new Expense(
                "Combustível",
                new BigDecimal("100.00"),
                "BRL",
                "usr_01",
                SplitType.EQUAL,
                List.of(
                        new ParticipantShare("usr_01", null),
                        new ParticipantShare("usr_02", null)
                )
        );
        assertDoesNotThrow(validExpense::validateSplitRules);
    }

    @Test
    @DisplayName("Deve aceitar regra PERCENTAGE quando a soma for exatamente 100%")
    void shouldAcceptValidPercentageSplitType() {
        Expense validExpense = new Expense(
                "Passeio",
                new BigDecimal("200.00"),
                "BRL",
                "usr_01",
                SplitType.PERCENTAGE,
                List.of(
                        new ParticipantShare("usr_01", new BigDecimal("70.00")),
                        new ParticipantShare("usr_02", new BigDecimal("30.00"))
                )
        );
        assertDoesNotThrow(validExpense::validateSplitRules);
    }

    @Test
    @DisplayName("Deve instanciar Group e Balance com tipos puros")
    void shouldInstantiateGroupAndBalance() {
        Participant p1 = new Participant("usr_01", "Ana");
        Participant p2 = new Participant("usr_02", "Bruno");
        Group group = new Group("grp_01", "Viagem Praia", List.of(p1, p2));

        assertEquals("grp_01", group.getId());
        assertEquals("Viagem Praia", group.getName());
        assertEquals(2, group.getParticipants().size());

        Balance balance = new Balance("usr_01", new BigDecimal("150.00"));
        assertEquals("usr_01", balance.getParticipantId());
        assertEquals(0, new BigDecimal("150.00").compareTo(balance.getAmount()));
    }

    @Test
    @DisplayName("Garante desacoplamento arquitetural: pacote expense.domain não deve importar trip, activity ou destination")
    void shouldEnsureArchitecturalDecoupling() throws IOException {
        Path domainDir = Paths.get("src/main/java/com/viajajunto/api/modules/expense/domain");
        assertTrue(Files.exists(domainDir), "O diretório do domínio deve existir");

        try (var stream = Files.walk(domainDir)) {
            List<Path> javaFiles = stream.filter(p -> p.toString().endsWith(".java")).toList();
            assertFalse(javaFiles.isEmpty(), "Deve haver classes no domínio de despesas");

            for (Path javaFile : javaFiles) {
                List<String> lines = Files.readAllLines(javaFile);
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("import ")) {
                        assertFalse(trimmed.contains(".trip"), "Não deve importar trip: " + javaFile.getFileName() + " -> " + trimmed);
                        assertFalse(trimmed.contains(".activity"), "Não deve importar activity: " + javaFile.getFileName() + " -> " + trimmed);
                        assertFalse(trimmed.contains(".destination"), "Não deve importar destination: " + javaFile.getFileName() + " -> " + trimmed);
                        assertFalse(trimmed.contains("jakarta.persistence"), "Não deve importar JPA: " + javaFile.getFileName() + " -> " + trimmed);
                        assertFalse(trimmed.contains("javax.persistence"), "Não deve importar JPA: " + javaFile.getFileName() + " -> " + trimmed);
                    }
                }
            }
        }
    }
}
