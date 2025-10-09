package org.transactions.digitalwallettraining.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.transactions.digitalwallettraining.config.WalletProperties;
import org.transactions.digitalwallettraining.model.WalletTransaction;

import java.util.ArrayList;
import java.util.List;

@Service
public class WalletService {

    private final TransactionProcessor transactionProcessor;
    private final WalletProperties walletProperties;

    private final List<WalletTransaction> transactions = new ArrayList<>();

    @Autowired
    public WalletService(TransactionProcessor transactionProcessor, WalletProperties walletProperties) {
        this.transactionProcessor = transactionProcessor;
        this.walletProperties = walletProperties;
    }

    public void process(List<WalletTransaction> transactions) {
        if (transactions == null) {
            throw new NullPointerException("Transaction list cannot be null");
        }

        List<WalletTransaction> validTransactions = transactions.stream()
                .filter(t -> t.amount() >= walletProperties.getMinAmount() &&
                        t.amount() <= walletProperties.getMaxAmount())
                .toList();

        transactionProcessor.processTransactions(validTransactions);
        this.transactions.addAll(validTransactions);
    }

    public int countActiveTransactions() {
        return transactions.size();
    }

    public void clearTransactions() {
        this.transactions.clear();
    }
}
