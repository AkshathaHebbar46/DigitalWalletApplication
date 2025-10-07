package org.transactions.digitalwallettraining.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.transactions.digitalwallettraining.model.WalletTransaction;
import org.transactions.digitalwallettraining.service.TransactionProcessor;

import java.util.ArrayList;
import java.util.List;

@Service
public class WalletService {

    private final TransactionProcessor transactionProcessor;

    private final List<WalletTransaction> transactions = new ArrayList<>();

    // Constructor-based Dependency Injection
    @Autowired
    public WalletService(TransactionProcessor transactionProcessor) {
        this.transactionProcessor = transactionProcessor;
    }

    public void process(List<WalletTransaction> transactions) {
        transactionProcessor.processTransactions(transactions);
        this.transactions.addAll(transactions); // now the internal list is updated

    }
    public int countActiveTransactions() {
        return transactions.size();
    }

}
