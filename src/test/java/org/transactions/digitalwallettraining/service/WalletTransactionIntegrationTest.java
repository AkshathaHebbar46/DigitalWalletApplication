package org.transactions.digitalwallettraining.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.transactions.digitalwallettraining.dto.*;
import org.transactions.digitalwallettraining.entity.*;
import org.transactions.digitalwallettraining.repository.*;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class WalletTransactionIntegrationTest {

    @Autowired private WalletService walletService;
    @Autowired private UserRepository userRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private TransactionRepository transactionRepository;

    private UserEntity user1;
    private UserEntity user2;
    private WalletEntity wallet1;
    private WalletEntity wallet2;

    @BeforeEach
    void setup() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();

        user1 = new UserEntity("Alice", "alice_" + System.nanoTime() + "@example.com", 25);
        user2 = new UserEntity("Bob", "bob_" + System.nanoTime() + "@example.com", 30);
        userRepository.saveAll(List.of(user1, user2));

        // ✅ Create wallets through the service to ensure consistency
        WalletResponseDTO w1 = walletService.createWallet(new WalletRequestDTO(user1.getId(), 1000.0));
        WalletResponseDTO w2 = walletService.createWallet(new WalletRequestDTO(user2.getId(), 2000.0));

        wallet1 = walletRepository.findById(w1.getWalletId()).orElseThrow();
        wallet2 = walletRepository.findById(w2.getWalletId()).orElseThrow();
    }

    @Test
    void testCreateWallet() {
        WalletRequestDTO request = new WalletRequestDTO(user1.getId(), 500.0);
        WalletResponseDTO response = walletService.createWallet(request);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(user1.getId());
        assertThat(response.getBalance()).isEqualTo(500.0);
    }

    @Test
    void testGetBalance() {
        Double balance = walletService.getBalance(wallet1.getId());
        assertThat(balance).isEqualTo(1000.0);
    }

    @Test
    void testCreditTransaction() {
        WalletTransactionRequestDTO request =
                new WalletTransactionRequestDTO("TXN1", 500.0, "CREDIT", "Deposit");

        WalletTransactionResponseDTO response =
                walletService.processTransaction(wallet1.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.amount()).isEqualTo(500.0);
        assertThat(walletService.getBalance(wallet1.getId())).isEqualTo(1500.0);
    }

    @Test
    void testDebitTransaction() {
        WalletTransactionRequestDTO request =
                new WalletTransactionRequestDTO("TXN2", 200.0, "DEBIT", "Purchase");

        WalletTransactionResponseDTO response =
                walletService.processTransaction(wallet1.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.amount()).isEqualTo(200.0);
        assertThat(walletService.getBalance(wallet1.getId())).isEqualTo(800.0);
    }

    @Test
    void testTransferMoney() {
        WalletTransactionResponseDTO response =
                walletService.transferMoney(wallet1.getId(), wallet2.getId(), 300.0);

        assertThat(response).isNotNull();
        assertThat(walletService.getBalance(wallet1.getId())).isEqualTo(700.0);
        assertThat(walletService.getBalance(wallet2.getId())).isEqualTo(2300.0);
    }

    @Test
    void testTransferMoneyInsufficientBalance() {
        assertThatThrownBy(() ->
                walletService.transferMoney(wallet1.getId(), wallet2.getId(), 5000.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    void testInvalidTransactionType() {
        WalletTransactionRequestDTO request =
                new WalletTransactionRequestDTO("TXN3", 100.0, "INVALID", "Test");
        assertThatThrownBy(() ->
                walletService.processTransaction(wallet1.getId(), request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testListTransactions() {
        WalletTransactionRequestDTO credit =
                new WalletTransactionRequestDTO("TXN4", 500.0, "CREDIT", "Deposit");
        walletService.processTransaction(wallet1.getId(), credit);

        WalletTransactionRequestDTO debit =
                new WalletTransactionRequestDTO("TXN5", 100.0, "DEBIT", "Shopping");
        walletService.processTransaction(wallet1.getId(), debit);

        List<WalletTransactionResponseDTO> txns =
                walletService.listTransactions(wallet1.getId());

        assertThat(txns).hasSize(2);
        assertThat(txns.get(0).description()).contains("Deposit");
        assertThat(txns.get(1).description()).contains("Shopping");
    }
}
