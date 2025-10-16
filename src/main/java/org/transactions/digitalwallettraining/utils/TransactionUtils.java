package org.transactions.digitalwallettraining.utils;

import org.transactions.digitalwallettraining.dto.WalletTransactionRequestDTO;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TransactionUtils {

    // Calculate total amount for a given type (CREDIT or DEBIT)
    public static double totalAmountByType(List<WalletTransactionRequestDTO> transactions, String type) {
        return transactions.stream()
                .filter(t -> t.type().equalsIgnoreCase(type))
                .mapToDouble(WalletTransactionRequestDTO::amount)
                .sum();
    }

    // Group transactions by type
    public static Map<String, List<WalletTransactionRequestDTO>> groupByType(List<WalletTransactionRequestDTO> transactions) {
        return transactions.stream()
                .collect(Collectors.groupingBy(t -> t.type().toUpperCase()));
    }
}
