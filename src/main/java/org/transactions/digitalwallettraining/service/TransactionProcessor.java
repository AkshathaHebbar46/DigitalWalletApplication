package org.transactions.digitalwallettraining.service;

import org.transactions.digitalwallettraining.model.WalletTransaction;
import org.transactions.digitalwallettraining.utils.TransactionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransactionProcessor {

    public void processTransactions(List<WalletTransaction> transactions) {

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        transactions.forEach(t -> {
            if (TransactionValidator.isValid(t)) {
                executor.submit(() ->
                        System.out.println("Processing transaction: " + t.transactionId() + " on " + Thread.currentThread())
                );
            } else {
                System.out.println("Invalid transaction skipped: " + t);
            }
        });
        executor.shutdown();

        double totalCredits = TransactionUtils.totalAmountByType(transactions, "CREDIT");
        double totalDebits = TransactionUtils.totalAmountByType(transactions, "DEBIT");
        Map<String, List<WalletTransaction>> grouped = TransactionUtils.groupByType(transactions);

        System.out.println("\n--- Transaction Summary ---");
        System.out.println("Total CREDIT amount: " + totalCredits);
        System.out.println("Total DEBIT amount: " + totalDebits);
        System.out.println("Transactions grouped by type: " + grouped.keySet());
    }
}
