package org.transactions.digitalwallettraining.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class WalletTransactionTest {

    // ✅ Valid transaction tests
    @Test
    void testValidCreditTransaction() {
        var txn = new WalletTransaction("TXN001", 100.0, "CREDIT", LocalDateTime.now());
        assertEquals("TXN001", txn.transactionId());
        assertEquals(100.0, txn.amount());
        assertEquals("CREDIT", txn.type());
        assertNotNull(txn.timestamp());
    }

    @Test
    void testValidDebitTransaction() {
        var txn = new WalletTransaction("TXN002", 50.5, "DEBIT", LocalDateTime.now());
        assertEquals("TXN002", txn.transactionId());
        assertEquals(50.5, txn.amount());
        assertEquals("DEBIT", txn.type());
    }

    @Test
    void testAutoTimestampIfNull() {
        var txn = new WalletTransaction("TXN003", 200.0, "CREDIT", null);
        assertNotNull(txn.timestamp());
    }

    // ✅ Boundary/Edge cases
    @Test
    void testMinimalPositiveAmount() {
        var txn = new WalletTransaction("TXN004", Double.MIN_VALUE, "CREDIT", LocalDateTime.now());
        assertEquals(Double.MIN_VALUE, txn.amount());
    }

    @Test
    void testVeryLargeAmount() {
        var txn = new WalletTransaction("TXN005", Double.MAX_VALUE, "DEBIT", LocalDateTime.now());
        assertEquals(Double.MAX_VALUE, txn.amount());
    }

    // ❌ Invalid inputs (should throw exceptions)
    @Test
    void testNullTransactionId() {
        var exception = assertThrows(IllegalArgumentException.class,
                () -> new WalletTransaction(null, 100, "CREDIT", LocalDateTime.now()));
        assertEquals("transactionId cannot be null", exception.getMessage());
    }

    @Test
    void testBlankTransactionId() {
        var exception = assertThrows(IllegalArgumentException.class,
                () -> new WalletTransaction(" ", 100, "CREDIT", LocalDateTime.now()));
        assertEquals("transactionId cannot be null", exception.getMessage());
    }

    @Test
    void testNegativeAmount() {
        var exception = assertThrows(IllegalArgumentException.class,
                () -> new WalletTransaction("TXN006", -10, "CREDIT", LocalDateTime.now()));
        assertEquals("amount cannot be negative", exception.getMessage());
    }

    @Test
    void testZeroAmount() {
        var exception = assertThrows(IllegalArgumentException.class,
                () -> new WalletTransaction("TXN007", 0, "DEBIT", LocalDateTime.now()));
        assertEquals("amount cannot be negative", exception.getMessage());
    }

    @Test
    void testInvalidType() {
        var exception = assertThrows(IllegalArgumentException.class,
                () -> new WalletTransaction("TXN008", 100, "TRANSFER", LocalDateTime.now()));
        assertEquals("type must be either CREDIT or DEBIT", exception.getMessage());
    }

    @Test
    void testNullType() {
        var exception = assertThrows(IllegalArgumentException.class,
                () -> new WalletTransaction("TXN009", 100, null, LocalDateTime.now()));
        assertEquals("type must be either CREDIT or DEBIT", exception.getMessage());
    }


}
