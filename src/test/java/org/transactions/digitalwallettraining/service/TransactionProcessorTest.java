package org.transactions.digitalwallettraining.service;

import org.junit.jupiter.api.Test;
import org.transactions.digitalwallettraining.model.WalletTransaction;

import java.time.LocalDateTime;
import java.util.List;

class TransactionProcessorTest {

    @Test
    void testProcessTransactions() {
        List<WalletTransaction> transactions = List.of(
                new WalletTransaction("TXN001", 10, "CREDIT", LocalDateTime.now()),
                new WalletTransaction("TXN002", 50000, "DEBIT", LocalDateTime.now())
        );

        TransactionProcessor processor = new TransactionProcessor();
        processor.processTransactions(transactions);
    }
}
