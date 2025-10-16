package org.transactions.digitalwallettraining.validation;

import org.transactions.digitalwallettraining.dto.WalletTransactionRequestDTO;

public class TransactionValidator {

    public static boolean isValid(WalletTransactionRequestDTO tx) {
        if (tx == null) return false;

        Double amount = tx.amount();
        String type = tx.type();

        return amount != null && amount > 0 &&
                (type.equalsIgnoreCase("CREDIT") || type.equalsIgnoreCase("DEBIT"));
    }
}
