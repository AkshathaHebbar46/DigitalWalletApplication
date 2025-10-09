package org.transactions.digitalwallettraining.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.transactions.digitalwallettraining.model.WalletTransaction;
import org.transactions.digitalwallettraining.service.WalletService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WalletPropertiesTest {

    @Autowired
    private WalletService walletService;

    @BeforeEach
    void resetWalletService() throws Exception {
        // Use reflection to clear the private transactions list before each test
        var field = WalletService.class.getDeclaredField("transactions");
        field.setAccessible(true);
        ((List<?>) field.get(walletService)).clear();
    }

    @Test
    void testTransactionsWithinMinMaxPass() {
        List<WalletTransaction> txns = List.of(
                new WalletTransaction("TXN001", 1, "CREDIT", LocalDateTime.now()),
                new WalletTransaction("TXN002", 50000, "DEBIT", LocalDateTime.now())
        );

        walletService.process(txns);

        assertEquals(2, walletService.countActiveTransactions());
    }

    @Test
    void testTransactionsBelowMinOrAboveMaxFail() {
        List<WalletTransaction> txns = List.of(
                new WalletTransaction("TXN003", 0.5, "CREDIT", LocalDateTime.now()),
                new WalletTransaction("TXN004", 60000, "DEBIT", LocalDateTime.now()),
                new WalletTransaction("TXN005", 100, "CREDIT", LocalDateTime.now())
        );

        walletService.process(txns);

        assertEquals(1, walletService.countActiveTransactions());
    }

    @Test
    void testMultipleValidTransactionsPass() {
        List<WalletTransaction> txns = List.of(
                new WalletTransaction("TXN006", 10, "CREDIT", LocalDateTime.now()),
                new WalletTransaction("TXN007", 5000, "DEBIT", LocalDateTime.now()),
                new WalletTransaction("TXN008", 30000, "CREDIT", LocalDateTime.now())
        );

        walletService.process(txns);

        assertEquals(3, walletService.countActiveTransactions());
    }

    @Test
    void testNullTransactionListThrowsException() {
        assertThrows(NullPointerException.class, () -> walletService.process(null));
    }

    @Test
    void testTransactionWithZeroAmountThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new WalletTransaction("TXN009", 0, "CREDIT", LocalDateTime.now())
        );
    }
}
