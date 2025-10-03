package org.transactions.digitalwallettraining.service;

import org.transactions.digitalwallettraining.model.WalletTransaction;

public class TransactionValidator {
    public static boolean isValid(Object obj) {
        if (obj instanceof WalletTransaction wt) {
            return wt.amount() > 0 && (wt.type().equals("CREDIT") || wt.type().equals("DEBIT"));
        }
        return false;
    }
}
