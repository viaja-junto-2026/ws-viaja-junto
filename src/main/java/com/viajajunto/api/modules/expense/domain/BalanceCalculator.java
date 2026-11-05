package com.viajajunto.api.modules.expense.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BalanceCalculator {

    private static class DebtEntry {
        private final String participantId;
        private BigDecimal remainingAmount;

        DebtEntry(String participantId, BigDecimal remainingAmount) {
            this.participantId = participantId;
            this.remainingAmount = remainingAmount;
        }
    }

    public static BalanceResult calculate(List<Expense> expenses, List<String> participantIds) {
        Map<String, BigDecimal> netBalances = new HashMap<>();
        if (participantIds != null) {
            participantIds.forEach(id -> netBalances.put(id, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)));
        }

        if (expenses != null) {
            for (Expense expense : expenses) {
                BigDecimal totalAmount = expense.getAmount();
                String payerId = expense.getPayerId();
                BigDecimal currentPayerBalance = netBalances.getOrDefault(payerId, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                netBalances.put(payerId, currentPayerBalance.add(totalAmount));

                List<ParticipantShare> shares = expense.getShares();
                if (shares == null || shares.isEmpty()) {
                    continue;
                }

                if (expense.getType() == SplitType.EQUAL) {
                    BigDecimal shareValue = totalAmount.divide(BigDecimal.valueOf(shares.size()), 2, RoundingMode.DOWN);
                    BigDecimal remainder = totalAmount.subtract(shareValue.multiply(BigDecimal.valueOf(shares.size())));

                    for (int i = 0; i < shares.size(); i++) {
                        String pid = shares.get(i).getParticipantId();
                        BigDecimal value = (i == 0) ? shareValue.add(remainder) : shareValue;
                        BigDecimal current = netBalances.getOrDefault(pid, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                        netBalances.put(pid, current.subtract(value));
                    }
                } else if (expense.getType() == SplitType.PERCENTAGE) {
                    for (ParticipantShare share : shares) {
                        BigDecimal value = totalAmount.multiply(share.getValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                        String pid = share.getParticipantId();
                        BigDecimal current = netBalances.getOrDefault(pid, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                        netBalances.put(pid, current.subtract(value));
                    }
                } else { // FIXED
                    for (ParticipantShare share : shares) {
                        String pid = share.getParticipantId();
                        BigDecimal current = netBalances.getOrDefault(pid, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                        netBalances.put(pid, current.subtract(share.getValue()));
                    }
                }
            }
        }

        List<Transfer> transfers = simplifyDebts(netBalances);
        return new BalanceResult(netBalances, transfers);
    }

    private static List<Transfer> simplifyDebts(Map<String, BigDecimal> netBalances) {
        List<Transfer> transfers = new ArrayList<>();
        List<DebtEntry> debtors = new ArrayList<>();
        List<DebtEntry> creditors = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> entry : netBalances.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) < 0) {
                debtors.add(new DebtEntry(entry.getKey(), entry.getValue().abs()));
            } else if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(new DebtEntry(entry.getKey(), entry.getValue()));
            }
        }

        int dIdx = 0;
        int cIdx = 0;

        while (dIdx < debtors.size() && cIdx < creditors.size()) {
            DebtEntry debtor = debtors.get(dIdx);
            DebtEntry creditor = creditors.get(cIdx);

            BigDecimal dAmount = debtor.remainingAmount;
            BigDecimal cAmount = creditor.remainingAmount;

            BigDecimal transferAmount = dAmount.min(cAmount);

            transfers.add(new Transfer(debtor.participantId, creditor.participantId, transferAmount));

            debtor.remainingAmount = debtor.remainingAmount.subtract(transferAmount);
            creditor.remainingAmount = creditor.remainingAmount.subtract(transferAmount);

            if (debtor.remainingAmount.compareTo(BigDecimal.ZERO) == 0) {
                dIdx++;
            }
            if (creditor.remainingAmount.compareTo(BigDecimal.ZERO) == 0) {
                cIdx++;
            }
        }

        return transfers;
    }
}
