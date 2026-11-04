
package com.viajajunto.api.modules.expense.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class BalanceCalculator {

    public static BalanceResult calculate(List<Expense> expenses, List<String> participantIds) {
        Map<String, BigDecimal> netBalances = new HashMap<>();
        participantIds.forEach(id -> netBalances.put(id, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)));

        for (Expense expense : expenses) {
            BigDecimal totalAmount = expense.getAmount();
            netBalances.put(expense.getPayerId(), netBalances.get(expense.getPayerId()).add(totalAmount));

            List<ParticipantShare> shares = expense.getShares();
            if (expense.getType() == SplitType.EQUAL) {
                BigDecimal shareValue = totalAmount.divide(BigDecimal.valueOf(shares.size()), 2, RoundingMode.DOWN);
                BigDecimal remainder = totalAmount.subtract(shareValue.multiply(BigDecimal.valueOf(shares.size())));

                for (int i = 0; i < shares.size(); i++) {
                    String pid = shares.get(i).getParticipantId();
                    BigDecimal value = (i == 0) ? shareValue.add(remainder) : shareValue;
                    netBalances.put(pid, netBalances.get(pid).subtract(value));
                }
            } else if (expense.getType() == SplitType.PERCENTAGE) {
                for (ParticipantShare share : shares) {
                    BigDecimal value = totalAmount.multiply(share.getValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                    netBalances.put(share.getParticipantId(), netBalances.get(share.getParticipantId()).subtract(value));
                }
            } else { // FIXED
                for (ParticipantShare share : shares) {
                    netBalances.put(share.getParticipantId(), netBalances.get(share.getParticipantId()).subtract(share.getValue()));
                }
            }
        }

        List<Transfer> transfers = simplifyDebts(netBalances);
        return new BalanceResult(netBalances, transfers);
    }

    private static List<Transfer> simplifyDebts(Map<String, BigDecimal> netBalances) {
        List<Transfer> transfers = new ArrayList<>();
        List<Map.Entry<String, BigDecimal>> debtors = new ArrayList<>();
        List<Map.Entry<String, BigDecimal>> creditors = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> entry : netBalances.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) < 0) {
                debtors.add(entry);
            } else if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(entry);
            }
        }

        int dIdx = 0;
        int cIdx = 0;

        while (dIdx < debtors.size() && cIdx < creditors.size()) {
            Map.Entry<String, BigDecimal> debtor = debtors.get(dIdx);
            Map.Entry<String, BigDecimal> creditor = creditors.get(cIdx);

            BigDecimal dAmount = debtor.getValue().abs();
            BigDecimal cAmount = creditor.getValue();

            BigDecimal transferAmount = dAmount.min(cAmount);

            transfers.add(new Transfer(debtor.getKey(), creditor.getKey(), transferAmount));

            debtor.setValue(debtor.getValue().add(transferAmount));
            creditor.setValue(creditor.getValue().subtract(transferAmount));

            if (debtor.getValue().compareTo(BigDecimal.ZERO) == 0) dIdx++;
            if (creditor.getValue().compareTo(BigDecimal.ZERO) == 0) cIdx++;
        }

        return transfers;
    }
}
