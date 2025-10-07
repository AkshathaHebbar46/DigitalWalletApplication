package org.transactions.digitalwallettraining.service;

import org.springframework.stereotype.Service;
import org.transactions.digitalwallettraining.model.WalletTransaction;
import org.transactions.digitalwallettraining.utils.TransactionUtils;
import org.transactions.digitalwallettraining.validation.TransactionValidator;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
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

        System.out.println("\nTransactions grouped by type:");
        grouped.forEach((type, txns) -> {
            System.out.print(type + " -> ");
            txns.forEach(t -> System.out.print(t.transactionId() + "(" + t.amount() + ") "));
            System.out.println();
        });


    }
}
