package org.transactions.digitalwallettraining.utils;

import org.transactions.digitalwallettraining.model.WalletTransaction;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TransactionUtils {
    public static List<WalletTransaction> filterByType(List<WalletTransaction> transactions, String type) {
        return transactions.stream()
                .filter(t -> t.type().equals(type))
                .toList();
    }
    public static double totalAmountByType(List<WalletTransaction> transactions, String type) {
        return transactions.stream()
                .filter(t -> t.type().equals(type))
                .mapToDouble(WalletTransaction::amount)
                .sum();
    }
    public static Map<String, List<WalletTransaction>> groupByType(List<WalletTransaction> transactions) {
        return transactions.stream()
                .collect(Collectors.groupingBy(WalletTransaction::type));
    }
}
