package org.transactions.digitalwallettraining.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.transactions.digitalwallettraining.model.WalletTransaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletServiceTest {

    private TransactionProcessor transactionProcessor;
    private WalletService walletService;

    @BeforeEach
    void setUp() {
        transactionProcessor = mock(TransactionProcessor.class);
        walletService = new WalletService(transactionProcessor);
    }

    @Test
    void testProcessDelegatesToTransactionProcessor() {
        List<WalletTransaction> transactions = List.of(
                new WalletTransaction("TXN001", 100, "CREDIT", LocalDateTime.now())
        );

        walletService.process(transactions);

        // Verify that processTransactions was called exactly once with the correct argument
        verify(transactionProcessor, times(1)).processTransactions(transactions);
    }

    @Test
    void testCountActiveTransactionsInitiallyZero() {
        // Initially, the transactions list should be empty
        assertEquals(0, walletService.countActiveTransactions());
    }

    @Test
    void testProcessAddToInternalListYet() {
        // The internal 'transactions' list in WalletService is not updated in your current process() method
        List<WalletTransaction> transactions = List.of(
                new WalletTransaction("TXN002", 50, "DEBIT", LocalDateTime.now())
        );

        walletService.process(transactions);

        assertEquals(1, walletService.countActiveTransactions());
    }

    @Test
    void testProcessNullTransactionList() {
        assertThrows(NullPointerException.class, () -> walletService.process(null));
    }

    @Test
    void testProcessLargeNumberOfTransactions() {
        List<WalletTransaction> txns = IntStream.range(0, 1000)
                .mapToObj(i -> new WalletTransaction("TXN" + i, 10, "CREDIT", LocalDateTime.now()))
                .collect(Collectors.toList());
        walletService.process(txns);
        assertEquals(1000, walletService.countActiveTransactions());
    }

    @Test
    void testProcessMixedValidAndInvalidTransactions() {
        // Check that negative transaction throws exception
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new WalletTransaction("TXN101", -10, "CREDIT", LocalDateTime.now())
        );
        assertEquals("amount cannot be zero or negative", exception.getMessage());

        // Process only valid transactions
        List<WalletTransaction> validTransactions = List.of(
                new WalletTransaction("TXN102", 50, "DEBIT", LocalDateTime.now())
        );

        walletService.process(validTransactions);

        // Assert that only valid transactions are counted
        assertEquals(1, walletService.countActiveTransactions());
    }


    @Test
    void testProcessZeroAmountTransactionThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            List<WalletTransaction> txns = List.of(
                    new WalletTransaction("TXN103", 0, "CREDIT", LocalDateTime.now())
            );
            walletService.process(txns);
        });

        assertEquals("amount cannot be zero or negative", exception.getMessage());
    }


}
