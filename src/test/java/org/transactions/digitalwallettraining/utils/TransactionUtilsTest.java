package org.transactions.digitalwallettraining.utils;

import org.junit.jupiter.api.Test;
import org.transactions.digitalwallettraining.dto.WalletTransactionRequestDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransactionUtilsTest {

    // Basic valid totals
    @Test
    void testTotalAmountByType() {
        List<WalletTransactionRequestDTO> transactions = List.of(
                new WalletTransactionRequestDTO("TXN001", 100.0, "CREDIT", "Desc1"),
                new WalletTransactionRequestDTO("TXN002", 50.0, "DEBIT", "Desc2"),
                new WalletTransactionRequestDTO("TXN003", 200.0, "CREDIT", "Desc3")
        );

        assertEquals(300.0, TransactionUtils.totalAmountByType(transactions, "CREDIT"));
        assertEquals(50.0, TransactionUtils.totalAmountByType(transactions, "DEBIT"));
    }

    // Grouping transactions by type
    @Test
    void testGroupByType() {
        List<WalletTransactionRequestDTO> transactions = List.of(
                new WalletTransactionRequestDTO("TXN001", 100.0, "CREDIT",  "Desc1"),
                new WalletTransactionRequestDTO("TXN002", 50.0, "DEBIT",  "Desc2"),
                new WalletTransactionRequestDTO("TXN003", 200.0, "CREDIT", "Desc3")
        );

        Map<String, List<WalletTransactionRequestDTO>> grouped = TransactionUtils.groupByType(transactions);

        assertEquals(2, grouped.get("CREDIT").size());
        assertEquals(1, grouped.get("DEBIT").size());
        assertTrue(grouped.containsKey("CREDIT"));
        assertTrue(grouped.containsKey("DEBIT"));
    }

    // Empty list should return 0 totals and empty group
    @Test
    void testEmptyTransactionList() {
        List<WalletTransactionRequestDTO> emptyList = List.of();

        assertEquals(0, TransactionUtils.totalAmountByType(emptyList, "CREDIT"));
        assertEquals(0, TransactionUtils.totalAmountByType(emptyList, "DEBIT"));
        assertTrue(TransactionUtils.groupByType(emptyList).isEmpty());
    }

    // Null list should throw NullPointerException
    @Test
    void testNullTransactionList() {
        assertThrows(NullPointerException.class, () -> TransactionUtils.totalAmountByType(null, "CREDIT"));
        assertThrows(NullPointerException.class, () -> TransactionUtils.groupByType(null));
    }
}
