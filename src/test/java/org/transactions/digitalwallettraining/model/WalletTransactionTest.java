package org.transactions.digitalwallettraining.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WalletTransactionTest {

    @Test
    void testWalletTransactionRecord() {
        var txn = new WalletTransaction("TXN001", 100, "CREDIT", LocalDateTime.now());
        assertEquals("TXN001", txn.transactionId());
        assertEquals(100, txn.amount());
        assertEquals("CREDIT", txn.type());
    }
}
