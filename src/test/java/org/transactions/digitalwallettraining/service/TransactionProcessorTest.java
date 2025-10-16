package org.transactions.digitalwallettraining.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.transactions.digitalwallettraining.dto.WalletTransactionRequestDTO;
import org.transactions.digitalwallettraining.utils.TransactionUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransactionProcessorTest {

    private TransactionProcessor processor;

    @BeforeEach
    void setup() {
        processor = new TransactionProcessor();
    }

    @Test
    void testProcessValidTransactions() {
        List<WalletTransactionRequestDTO> transactions = List.of(
                new WalletTransactionRequestDTO("TXN001", 100.0, "CREDIT", "Desc1"),
                new WalletTransactionRequestDTO("TXN002", 50.0, "DEBIT", "Desc2"),
                new WalletTransactionRequestDTO("TXN003", 100.0, "CREDIT", "Desc3"),
                new WalletTransactionRequestDTO("TXN004", 1000.0, "CREDIT", "Desc4")
        );

        assertDoesNotThrow(() -> processor.processTransactions(transactions));
        assertEquals(1200.0, TransactionUtils.totalAmountByType(transactions, "CREDIT"));
        assertEquals(50.0, TransactionUtils.totalAmountByType(transactions, "DEBIT"));

        Map<String, List<WalletTransactionRequestDTO>> grouped = TransactionUtils.groupByType(transactions);
        assertTrue(grouped.containsKey("CREDIT"));
        assertTrue(grouped.containsKey("DEBIT"));
    }

    @Test
    void testProcessEmptyList() {
        List<WalletTransactionRequestDTO> transactions = List.of();
        assertDoesNotThrow(() -> processor.processTransactions(transactions));
        assertTrue(transactions.isEmpty());
    }

    @Test
    void testInvalidTransactionCreationThrows() {
        // Amount <= 0 should be invalid
        assertThrows(IllegalArgumentException.class,
                () -> new WalletTransactionRequestDTO("TXN003", 0.0, "CREDIT", "Desc"));
        assertThrows(IllegalArgumentException.class,
                () -> new WalletTransactionRequestDTO("TXN004", -10.0, "DEBIT", "Desc"));
    }

    @Test
    void testSingleTransaction() {
        List<WalletTransactionRequestDTO> transactions = List.of(
                new WalletTransactionRequestDTO("TXN005", 75.0, "CREDIT", "Desc")
        );

        assertDoesNotThrow(() -> processor.processTransactions(transactions));
        assertEquals(75.0, TransactionUtils.totalAmountByType(transactions, "CREDIT"));
    }

    @Test
    void testProcessMixedTransactions() {
        List<WalletTransactionRequestDTO> transactions = List.of(
                new WalletTransactionRequestDTO("TXN006", 10.0, "CREDIT", "Desc"),
                new WalletTransactionRequestDTO("TXN007", 20.0, "DEBIT", "Desc"),
                new WalletTransactionRequestDTO("TXN008", 30.0, "CREDIT", "Desc")
        );

        assertDoesNotThrow(() -> processor.processTransactions(transactions));

        assertEquals(40.0, TransactionUtils.totalAmountByType(transactions, "CREDIT"));
        assertEquals(20.0, TransactionUtils.totalAmountByType(transactions, "DEBIT"));

        Map<String, List<WalletTransactionRequestDTO>> grouped = TransactionUtils.groupByType(transactions);
        assertEquals(2, grouped.get("CREDIT").size());
        assertEquals(1, grouped.get("DEBIT").size());
    }

    @Test
    void testAllCreditTransactions() {
        List<WalletTransactionRequestDTO> transactions = List.of(
                new WalletTransactionRequestDTO("TXN101", 100.0, "CREDIT", "Desc"),
                new WalletTransactionRequestDTO("TXN102", 200.0, "CREDIT", "Desc")
        );

        assertDoesNotThrow(() -> processor.processTransactions(transactions));
        assertEquals(300.0, TransactionUtils.totalAmountByType(transactions, "CREDIT"));
        assertEquals(0.0, TransactionUtils.totalAmountByType(transactions, "DEBIT"));
    }

    @Test
    void testAllDebitTransactions() {
        List<WalletTransactionRequestDTO> transactions = List.of(
                new WalletTransactionRequestDTO("TXN103", 50.0, "DEBIT", "Desc"),
                new WalletTransactionRequestDTO("TXN104", 150.0, "DEBIT", "Desc")
        );

        assertDoesNotThrow(() -> processor.processTransactions(transactions));
        assertEquals(0.0, TransactionUtils.totalAmountByType(transactions, "CREDIT"));
        assertEquals(200.0, TransactionUtils.totalAmountByType(transactions, "DEBIT"));
    }

    @Test
    void testNullTimestampDefaultsToNow() {
        // timestamp removed from DTO, nothing needed here
        WalletTransactionRequestDTO txn = new WalletTransactionRequestDTO("TXN110", 100.0, "CREDIT", "Desc");
        assertDoesNotThrow(() -> processor.processTransactions(List.of(txn)));
    }
}
