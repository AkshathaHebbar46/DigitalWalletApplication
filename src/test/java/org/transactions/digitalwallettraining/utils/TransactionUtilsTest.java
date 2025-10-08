package org.transactions.digitalwallettraining.utils;

import org.junit.jupiter.api.Test;
import org.transactions.digitalwallettraining.model.WalletTransaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransactionUtilsTest {

    // Basic valid totals
    @Test
    void testTotalAmountByType() {
        List<WalletTransaction> transactions = List.of(
                new WalletTransaction("TXN001", 100, "CREDIT", LocalDateTime.now()),
                new WalletTransaction("TXN002", 50, "DEBIT", LocalDateTime.now()),
                new WalletTransaction("TXN003", 200, "CREDIT", LocalDateTime.now())
        );

        assertEquals(300, TransactionUtils.totalAmountByType(transactions, "CREDIT"));
        assertEquals(50, TransactionUtils.totalAmountByType(transactions, "DEBIT"));
    }

    // Grouping transactions by type
    @Test
    void testGroupByType() {
        List<WalletTransaction> transactions = List.of(
                new WalletTransaction("TXN001", 100, "CREDIT", LocalDateTime.now()),
                new WalletTransaction("TXN002", 50, "DEBIT", LocalDateTime.now()),
                new WalletTransaction("TXN003", 200, "CREDIT", LocalDateTime.now())
        );

        Map<String, List<WalletTransaction>> grouped = TransactionUtils.groupByType(transactions);

        assertEquals(2, grouped.get("CREDIT").size());
        assertEquals(1, grouped.get("DEBIT").size());
        assertTrue(grouped.containsKey("CREDIT"));
        assertTrue(grouped.containsKey("DEBIT"));
    }

    // Empty list should return 0 totals and empty group
    @Test
    void testEmptyTransactionList() {
        List<WalletTransaction> emptyList = List.of();

        assertEquals(0, TransactionUtils.totalAmountByType(emptyList, "CREDIT"));
        assertEquals(0, TransactionUtils.totalAmountByType(emptyList, "DEBIT"));
        assertTrue(TransactionUtils.groupByType(emptyList).isEmpty());
    }

    // Transactions with zero amount (boundary case)
    @Test
    void testZeroAmountTransactionThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new WalletTransaction("TXN010", 0, "CREDIT", LocalDateTime.now());
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new WalletTransaction("TXN011", 0, "DEBIT", LocalDateTime.now());
        });
    }


    // Transactions with only one type present
    @Test
    void testSingleTypeTransactions() {
        List<WalletTransaction> transactions = List.of(
                new WalletTransaction("TXN020", 100, "CREDIT", LocalDateTime.now()),
                new WalletTransaction("TXN021", 200, "CREDIT", LocalDateTime.now())
        );

        assertEquals(300, TransactionUtils.totalAmountByType(transactions, "CREDIT"));
        assertEquals(0, TransactionUtils.totalAmountByType(transactions, "DEBIT"));

        Map<String, List<WalletTransaction>> grouped = TransactionUtils.groupByType(transactions);
        assertEquals(2, grouped.get("CREDIT").size());
        assertFalse(grouped.containsKey("DEBIT"));
    }

    // Case where type requested does not exist
    @Test
    void testTotalAmountForNonExistentType() {
        List<WalletTransaction> transactions = List.of(
                new WalletTransaction("TXN030", 100, "CREDIT", LocalDateTime.now())
        );

        assertEquals(0, TransactionUtils.totalAmountByType(transactions, "DEBIT"));
    }


    // Large transaction amounts (boundary case)
    @Test
    void testLargeAmounts() {
        List<WalletTransaction> transactions = List.of(
                new WalletTransaction("TXN050", Double.MAX_VALUE, "CREDIT", LocalDateTime.now()),
                new WalletTransaction("TXN051", Double.MAX_VALUE, "DEBIT", LocalDateTime.now())
        );

        assertEquals(Double.MAX_VALUE, TransactionUtils.totalAmountByType(transactions, "CREDIT"));
        assertEquals(Double.MAX_VALUE, TransactionUtils.totalAmountByType(transactions, "DEBIT"));
    }

    // Null list should throw NullPointerException
    @Test
    void testNullTransactionList() {
        assertThrows(NullPointerException.class, () -> TransactionUtils.totalAmountByType(null, "CREDIT"));
        assertThrows(NullPointerException.class, () -> TransactionUtils.groupByType(null));
    }
}
