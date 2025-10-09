package org.transactions.digitalwallettraining.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class WalletTransactionTest {

    @Test
    void testValidWalletTransaction() {
        WalletTransaction txn = new WalletTransaction("TXN001", 100, "CREDIT", LocalDateTime.now());
        assertEquals("TXN001", txn.transactionId());
        assertEquals(100, txn.amount());
        assertEquals("CREDIT", txn.type());
        assertNotNull(txn.timestamp());
    }

    @Test
    void testTransactionIdCannotBeNullOrBlank() {
        // Null
        Exception ex1 = assertThrows(IllegalArgumentException.class, () ->
                new WalletTransaction(null, 100, "CREDIT", LocalDateTime.now()));
        assertEquals("transactionId cannot be null", ex1.getMessage());

        // Blank
        Exception ex2 = assertThrows(IllegalArgumentException.class, () ->
                new WalletTransaction("   ", 100, "CREDIT", LocalDateTime.now()));
        assertEquals("transactionId cannot be null", ex2.getMessage());
    }

    @Test
    void testAmountCannotBeZeroOrNegative() {
        Exception ex1 = assertThrows(IllegalArgumentException.class, () ->
                new WalletTransaction("TXN002", 0, "CREDIT", LocalDateTime.now()));
        assertEquals("amount cannot be zero or negative", ex1.getMessage());

        Exception ex2 = assertThrows(IllegalArgumentException.class, () ->
                new WalletTransaction("TXN003", -10, "DEBIT", LocalDateTime.now()));
        assertEquals("amount cannot be zero or negative", ex2.getMessage());
    }

    @Test
    void testTypeMustBeCreditOrDebit() {
        Exception ex1 = assertThrows(IllegalArgumentException.class, () ->
                new WalletTransaction("TXN004", 100, null, LocalDateTime.now()));
        assertEquals("type must be either CREDIT or DEBIT", ex1.getMessage());

        Exception ex2 = assertThrows(IllegalArgumentException.class, () ->
                new WalletTransaction("TXN005", 100, "TRANSFER", LocalDateTime.now()));
        assertEquals("type must be either CREDIT or DEBIT", ex2.getMessage());
    }

    @Test
    void testTimestampDefaultsToNowIfNull() {
        WalletTransaction txn = new WalletTransaction("TXN006", 50, "CREDIT", null);
        assertNotNull(txn.timestamp());
        assertTrue(txn.timestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
    }
}
