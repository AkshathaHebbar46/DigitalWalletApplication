package org.transactions.digitalwallettraining.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.transactions.digitalwallettraining.dto.WalletTransactionRequestDTO;
import org.transactions.digitalwallettraining.dto.WalletTransactionResponseDTO;
import org.transactions.digitalwallettraining.dto.WalletRequestDTO;
import org.transactions.digitalwallettraining.dto.WalletResponseDTO;
import org.transactions.digitalwallettraining.entity.UserEntity;
import org.transactions.digitalwallettraining.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WalletServiceTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testValidTransactionProcessing() {
        // Create a test user with age
        UserEntity user = new UserEntity("TestUser", "testuser@example.com", 25);
        userRepository.save(user);

        // Create a test wallet
        WalletRequestDTO walletDto = new WalletRequestDTO(user.getId(), 500.0);
        WalletResponseDTO wallet = walletService.createWallet(walletDto);

        // Create a credit transaction
        WalletTransactionRequestDTO txDto = new WalletTransactionRequestDTO(
                "TXN001",
                1000.0,
                "CREDIT",
                "Salary"
        );

        WalletTransactionResponseDTO response = walletService.processTransaction(wallet.getWalletId(), txDto);

        assertNotNull(response.transactionId());
        assertEquals(1000.0, response.amount());
        assertEquals("CREDIT", response.type());

        // Verify wallet balance updated
        Double updatedBalance = walletService.getBalance(wallet.getWalletId());
        assertEquals(1500.0, updatedBalance);
    }

    @Test
    void testInvalidTransactionThrowsException() {
        // Create a test user with age
        UserEntity user = new UserEntity("TestUser2", "testuser2@example.com", 30);
        userRepository.save(user);

        // Create a test wallet
        WalletRequestDTO walletDto = new WalletRequestDTO(user.getId(), 100.0);
        WalletResponseDTO wallet = walletService.createWallet(walletDto);

        // Create a debit transaction larger than balance
        WalletTransactionRequestDTO txDto = new WalletTransactionRequestDTO(
                "TXN002",
                200.0,
                "DEBIT",
                "Invalid"
        );

        // Expect exception due to insufficient balance
        assertThrows(IllegalArgumentException.class, () ->
                walletService.processTransaction(wallet.getWalletId(), txDto)
        );
    }
}
