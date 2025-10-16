package org.transactions.digitalwallettraining.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.transactions.digitalwallettraining.dto.WalletTransactionRequestDTO;
import org.transactions.digitalwallettraining.dto.WalletTransactionResponseDTO;
import org.transactions.digitalwallettraining.dto.WalletRequestDTO;
import org.transactions.digitalwallettraining.dto.WalletResponseDTO;
import org.transactions.digitalwallettraining.entity.UserEntity;
import org.transactions.digitalwallettraining.repository.UserRepository;
import org.transactions.digitalwallettraining.service.WalletService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WalletPropertiesTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserRepository userRepository;

    private WalletResponseDTO wallet;

    @BeforeEach
    void setupWallet() {
        // Create a test user
        UserEntity user = new UserEntity("TestUser", "testuser@example.com", 25);
        userRepository.save(user);

        // Create a test wallet with some initial balance
        WalletRequestDTO walletDto = new WalletRequestDTO(user.getId(), 1000.0);
        wallet = walletService.createWallet(walletDto);
    }

    @Test
    void testValidTransactionsPass() {
        List<WalletTransactionRequestDTO> txns = List.of(
                new WalletTransactionRequestDTO("TXN001", 100.0, "CREDIT", "Salary"),
                new WalletTransactionRequestDTO("TXN002", 50.0, "DEBIT", "Groceries")
        );

        txns.forEach(tx -> walletService.processTransaction(wallet.getWalletId(), tx));

        List<WalletTransactionResponseDTO> processed = walletService.listTransactions(wallet.getWalletId());
        assertEquals(2, processed.size());
    }

    @Test
    void testTransactionExceedingBalanceFails() {
        List<WalletTransactionRequestDTO> txns = List.of(
                new WalletTransactionRequestDTO("TXN003", 5000.0, "DEBIT", "Rent")
        );

        assertThrows(IllegalArgumentException.class, () ->
                txns.forEach(tx -> walletService.processTransaction(wallet.getWalletId(), tx))
        );
    }

    @Test
    void testMultipleValidTransactionsPass() {
        List<WalletTransactionRequestDTO> txns = List.of(
                new WalletTransactionRequestDTO("TXN004", 200.0, "CREDIT",  "Bonus"),
                new WalletTransactionRequestDTO("TXN005", 150.0, "DEBIT", "Shopping"),
                new WalletTransactionRequestDTO("TXN006", 300.0, "CREDIT", "Gift")
        );

        txns.forEach(tx -> walletService.processTransaction(wallet.getWalletId(), tx));

        List<WalletTransactionResponseDTO> processed = walletService.listTransactions(wallet.getWalletId());
        assertEquals(3, processed.size());
    }

    @Test
    void testTransactionWithZeroAmountThrowsException() {
        WalletTransactionRequestDTO tx = new WalletTransactionRequestDTO(
                "TXN007", 0.0, "CREDIT", "Invalid"
        );

        assertThrows(IllegalArgumentException.class,
                () -> walletService.processTransaction(wallet.getWalletId(), tx));
    }

    @Test
    void testTransactionWithNegativeAmountThrowsException() {
        WalletTransactionRequestDTO tx = new WalletTransactionRequestDTO(
                "TXN008", -50.0, "DEBIT",  "Invalid"
        );

        assertThrows(IllegalArgumentException.class,
                () -> walletService.processTransaction(wallet.getWalletId(), tx));
    }
}
