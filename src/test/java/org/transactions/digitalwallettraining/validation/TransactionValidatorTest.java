package org.transactions.digitalwallettraining.validation;

import org.junit.jupiter.api.Test;
import org.transactions.digitalwallettraining.model.WalletTransaction;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransactionValidatorTest {

    @Test
    void testValidCreditTransaction() {
        WalletTransaction txn = new WalletTransaction("TXN001", 100, "CREDIT", LocalDateTime.now());
        assertTrue(TransactionValidator.isValid(txn));
    }

    @Test
    void testValidDebitTransaction() {
        WalletTransaction txn = new WalletTransaction("TXN002", 50, "DEBIT", LocalDateTime.now());
        assertTrue(TransactionValidator.isValid(txn));
    }


    @Test
    void testNegativeAmountTransaction() {
        var exception = assertThrows(IllegalArgumentException.class,
                () -> new WalletTransaction("TXN999", -100, "CREDIT", LocalDateTime.now()));
        assertEquals("amount cannot be zero or negative", exception.getMessage());
    }
    @Test
    void testNonTransactionObject() {
        String str = "Not a transaction";
        assertFalse(TransactionValidator.isValid(str));
    }

    @Test
    void testNullObject() {
        assertFalse(TransactionValidator.isValid(null));
    }

    @Test
    void testInvalidTypeTransaction() {
        var exception = assertThrows(IllegalArgumentException.class,
                () -> new WalletTransaction("TXN999", 100, "TRANSFER", LocalDateTime.now()));
        assertEquals("type must be either CREDIT or DEBIT", exception.getMessage());
    }
}
