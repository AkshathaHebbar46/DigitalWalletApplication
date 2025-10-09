package org.transactions.digitalwallettraining.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.transactions.digitalwallettraining.model.WalletTransaction;
import org.transactions.digitalwallettraining.utils.TransactionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;

class TransactionProcessorTest {

    private TransactionProcessor processor;

    @BeforeEach
    void setup() {
        processor = new TransactionProcessor();
    }

    // Normal transactions
    @Test
    void testProcessValidTransactions() {
        List<WalletTransaction> transactions = List.of(
                new WalletTransaction("TXN001", 100, "CREDIT", LocalDateTime.now()),
                new WalletTransaction("TXN002", 50, "DEBIT", LocalDateTime.now()),
                new WalletTransaction("TXN003", 100, "CREDIT", LocalDateTime.now()),
                new WalletTransaction("TXN001", 1000.0, "CREDIT", LocalDateTime.now())
                );

        assertDoesNotThrow(() -> processor.processTransactions(transactions));
        assertEquals(1200.0, TransactionUtils.totalAmountByType(transactions, "CREDIT"));
        assertEquals(50, TransactionUtils.totalAmountByType(transactions, "DEBIT"));

        Map<String, List<WalletTransaction>> grouped = TransactionUtils.groupByType(transactions);
        assertTrue(grouped.containsKey("CREDIT"));
        assertTrue(grouped.containsKey("DEBIT"));
    }

    //  Empty transaction list
    @Test
    void testProcessEmptyList() {
        List<WalletTransaction> transactions = List.of();
        assertDoesNotThrow(() -> processor.processTransactions(transactions));
        assertTrue(transactions.isEmpty());
    }

    @Test
    void testInvalidTransactionCreationThrows() {
        // Test that creating a transaction with negative amount throws an exception
        var exception = assertThrows(IllegalArgumentException.class,
                () -> new WalletTransaction("TXN003", -10, "CREDIT", LocalDateTime.now()));
        assertEquals("amount cannot be zero or negative", exception.getMessage());

        // Valid transaction should still work
        assertDoesNotThrow(() -> new WalletTransaction("TXN004", 50, "DEBIT", LocalDateTime.now()));
    }


    @Test
    void testProcessHugeListInBatches() {
        TransactionProcessor processor = new TransactionProcessor();
        long totalTransactions = 100_000_000;
        long batchSize = 1_000_000;

        for (long start = 1; start <= totalTransactions; start += batchSize) {
            long end = Math.min(start + batchSize - 1, totalTransactions);
            List<WalletTransaction> batch = LongStream.rangeClosed(start, end)
                    .mapToObj(i -> new WalletTransaction(
                            "TXN" + i,
                            i,
                            i % 2 == 0 ? "CREDIT" : "DEBIT",
                            LocalDateTime.now()
                    ))
                    .collect(Collectors.toList());

            // Process each batch safely
            assertDoesNotThrow(() -> processor.processTransactions(batch));
        }
    }


    // Single transaction
    @Test
    void testSingleTransaction() {
        List<WalletTransaction> transactions = List.of(
                new WalletTransaction("TXN005", 75, "CREDIT", LocalDateTime.now())
        );
        assertDoesNotThrow(() -> processor.processTransactions(transactions));
        assertEquals(75, TransactionUtils.totalAmountByType(transactions, "CREDIT"));
    }

    // Mixed credits and debits
    @Test
    void testProcessMixedTransactions() {
        List<WalletTransaction> transactions = List.of(
                new WalletTransaction("TXN006", 10, "CREDIT", LocalDateTime.now()),
                new WalletTransaction("TXN007", 20, "DEBIT", LocalDateTime.now()),
                new WalletTransaction("TXN008", 30, "CREDIT", LocalDateTime.now())
        );
        assertDoesNotThrow(() -> processor.processTransactions(transactions));

        assertEquals(40, TransactionUtils.totalAmountByType(transactions, "CREDIT"));
        assertEquals(20, TransactionUtils.totalAmountByType(transactions, "DEBIT"));

        Map<String, List<WalletTransaction>> grouped = TransactionUtils.groupByType(transactions);
        assertEquals(2, grouped.get("CREDIT").size());
        assertEquals(1, grouped.get("DEBIT").size());
    }

    @Test
    void testAllCreditTransactions() {
        List<WalletTransaction> transactions = List.of(
                new WalletTransaction("TXN101", 100, "CREDIT", LocalDateTime.now()),
                new WalletTransaction("TXN102", 200, "CREDIT", LocalDateTime.now())
        );

        assertDoesNotThrow(() -> processor.processTransactions(transactions));
        assertEquals(300, TransactionUtils.totalAmountByType(transactions, "CREDIT"));
        assertEquals(0, TransactionUtils.totalAmountByType(transactions, "DEBIT"));
    }

    @Test
    void testAllDebitTransactions() {
        List<WalletTransaction> transactions = List.of(
                new WalletTransaction("TXN103", 50, "DEBIT", LocalDateTime.now()),
                new WalletTransaction("TXN104", 150, "DEBIT", LocalDateTime.now())
        );

        assertDoesNotThrow(() -> processor.processTransactions(transactions));
        assertEquals(0, TransactionUtils.totalAmountByType(transactions, "CREDIT"));
        assertEquals(200, TransactionUtils.totalAmountByType(transactions, "DEBIT"));
    }

    @Test
    void testPastTimestamps() {
        List<WalletTransaction> transactions = List.of(
                new WalletTransaction("TXN106", 200, "CREDIT", LocalDateTime.now().minusDays(1)),
                new WalletTransaction("TXN107", 100, "DEBIT", LocalDateTime.now().minusHours(5))
        );

        assertDoesNotThrow(() -> processor.processTransactions(transactions));
    }

    @Test
    void testMixedValidInvalidTransactions() {
        // Invalid transactions (amount negative)
        var ex1 = assertThrows(IllegalArgumentException.class,
                () -> new WalletTransaction("TXN101", -10, "CREDIT", LocalDateTime.now()));
        assertEquals("amount cannot be zero or negative", ex1.getMessage());

        var ex2 = assertThrows(IllegalArgumentException.class,
                () -> new WalletTransaction("TXN102", -20, "DEBIT", LocalDateTime.now()));
        assertEquals("amount cannot be zero or negative", ex2.getMessage());

        // Valid transactions
        WalletTransaction txn1 = new WalletTransaction("TXN103", 50, "CREDIT", LocalDateTime.now());
        WalletTransaction txn2 = new WalletTransaction("TXN104", 100, "DEBIT", LocalDateTime.now());

        // Process only valid transactions
        List<WalletTransaction> validTransactions = List.of(txn1, txn2);
        assertDoesNotThrow(() -> processor.processTransactions(validTransactions));

        // Check totals
        assertEquals(50, TransactionUtils.totalAmountByType(validTransactions, "CREDIT"));
        assertEquals(100, TransactionUtils.totalAmountByType(validTransactions, "DEBIT"));

        // Check grouping
        Map<String, List<WalletTransaction>> grouped = TransactionUtils.groupByType(validTransactions);
        assertEquals(1, grouped.get("CREDIT").size());
        assertEquals(1, grouped.get("DEBIT").size());
    }

    @Test
    void testNullTimestamp() {
        WalletTransaction txn = new WalletTransaction("TXN110", 100, "CREDIT", null);

        assertNotNull(txn.timestamp(), "Timestamp should be auto-set if null");
        assertDoesNotThrow(() -> processor.processTransactions(List.of(txn)));
    }







}
