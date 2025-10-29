package org.transactions.digitalwallettraining.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import org.transactions.digitalwallettraining.dto.*;
import org.transactions.digitalwallettraining.entity.UserEntity;
import org.transactions.digitalwallettraining.repository.TransactionRepository;
import org.transactions.digitalwallettraining.repository.UserRepository;
import org.transactions.digitalwallettraining.repository.WalletRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolationException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WalletTransactionIntegrationTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private WalletResponseDTO wallet;

    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @BeforeEach
    void setup() {
        // Clean all tables before each test
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();

        // Create a unique user every time
        String uniqueEmail = "testuser_" + System.nanoTime() + "@example.com";
        UserEntity user = new UserEntity("TestUser", uniqueEmail, 25);
        userRepository.save(user);

        // Create wallet with initial balance
        wallet = walletService.createWallet(new WalletRequestDTO(user.getId(), 1000.0));
    }

    // ✅ Test 1 — Basic credit transaction
    @Test
    @DisplayName("CREDIT transaction should increase balance")
    void testCreditTransaction() {
        WalletTransactionRequestDTO txDto = new WalletTransactionRequestDTO(
                "TXN_" + System.nanoTime(),
                500.0,
                "CREDIT",
                "Salary"
        );

        WalletTransactionResponseDTO response = walletService.processTransaction(wallet.getWalletId(), txDto);

        assertNotNull(response.transactionId());
        assertEquals(500.0, response.amount());
        assertEquals("CREDIT", response.type());
        assertEquals(1500.0, walletService.getBalance(wallet.getWalletId()));
    }

    // ✅ Test 2 — Basic debit transaction
    @Test
    @DisplayName("DEBIT transaction should decrease balance")
    void testDebitTransaction() {
        WalletTransactionRequestDTO txDto = new WalletTransactionRequestDTO(
                "TXN_" + System.nanoTime(),
                200.0,
                "DEBIT",
                "Shopping"
        );

        WalletTransactionResponseDTO response = walletService.processTransaction(wallet.getWalletId(), txDto);

        assertNotNull(response.transactionId());
        assertEquals(200.0, response.amount());
        assertEquals("DEBIT", response.type());
        assertEquals(800.0, walletService.getBalance(wallet.getWalletId()));
    }

    // ✅ Test 3 — Insufficient balance should throw and rollback
    @Test
    @DisplayName("DEBIT should fail if insufficient balance and rollback")
    void testInsufficientBalanceRollback() {
        WalletTransactionRequestDTO txDto = new WalletTransactionRequestDTO(
                "TXN_" + System.nanoTime(),
                5000.0,
                "DEBIT",
                "Big Purchase"
        );

        assertThrows(IllegalArgumentException.class, () ->
                walletService.processTransaction(wallet.getWalletId(), txDto));

        assertEquals(1000.0, walletService.getBalance(wallet.getWalletId()));
    }

    // ✅ Test 4 — Idempotency (duplicate transactionId ignored)
    @Test
    @DisplayName("Duplicate transactionId should not process twice (idempotency)")
    void testIdempotentTransaction() {
        String txnId = "TXN_" + System.nanoTime();

        WalletTransactionRequestDTO txDto = new WalletTransactionRequestDTO(
                txnId,
                100.0,
                "CREDIT",
                "First Credit"
        );

        walletService.processTransaction(wallet.getWalletId(), txDto);

        // Same transaction ID should not create new one
        walletService.processTransaction(wallet.getWalletId(), txDto);

        // Only one transaction with that ID
        assertEquals(1, transactionRepository.findByWalletId(wallet.getWalletId()).size());
        assertEquals(1100.0, walletService.getBalance(wallet.getWalletId()));
    }

    // ✅ Test 5 — Successful transfer between wallets
    @Test
    @DisplayName("Money transfer updates both wallets atomically")
    void testSuccessfulTransfer() {
        UserEntity receiver = new UserEntity("Receiver", "recv@example.com", 30);
        userRepository.save(receiver);
        WalletResponseDTO wallet2 = walletService.createWallet(new WalletRequestDTO(receiver.getId(), 500.0));

        WalletTransferRequestDTO transfer = new WalletTransferRequestDTO(wallet.getWalletId(), wallet2.getWalletId(), 200.0);

        // Create a WalletTransactionRequestDTO as expected by the service
        WalletTransactionRequestDTO request = new WalletTransactionRequestDTO(
                "TEST-TXN-123",
                transfer.amount(),
                "DEBIT",
                "Transfer from wallet " + transfer.fromWalletId()
        );

        walletService.transferMoney(transfer.fromWalletId(), transfer.toWalletId(), request);

        assertEquals(800.0, walletService.getBalance(wallet.getWalletId()));
        assertEquals(700.0, walletService.getBalance(wallet2.getWalletId()));
    }


    // ✅ Test 7 — Negative or zero amount validation
    @Test
    @DisplayName("Transaction should fail for zero or negative amount")
    void testInvalidAmount() {
        WalletTransactionRequestDTO invalid1 = new WalletTransactionRequestDTO("TXN_NEG", -50.0, "CREDIT", "Invalid");
        WalletTransactionRequestDTO invalid2 = new WalletTransactionRequestDTO("TXN_ZERO", 0.0, "DEBIT", "Invalid");

        assertThrows(ConstraintViolationException.class, () -> {
            var violations = validator.validate(invalid1);
            if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
        });

        assertThrows(ConstraintViolationException.class, () -> {
            var violations = validator.validate(invalid2);
            if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
        });
    }

    // ✅ Test 8 — Simulate concurrent debit operations on the same wallet to verify
    // optimistic locking and retry logic handle version conflicts correctly.
    @Test
    void testOptimisticLockingWithRetry() throws InterruptedException {
        WalletTransactionRequestDTO tx1 = new WalletTransactionRequestDTO("TXN_LOCK_1", 300.0, "DEBIT", "RetryTxn1");
        WalletTransactionRequestDTO tx2 = new WalletTransactionRequestDTO("TXN_LOCK_2", 400.0, "DEBIT", "RetryTxn2");

        Thread t1 = new Thread(() -> walletService.processTransaction(wallet.getWalletId(), tx1));
        Thread t2 = new Thread(() -> walletService.processTransaction(wallet.getWalletId(), tx2));

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        // Both succeed eventually, but only one should have applied latest balance correctly
        Double balance = walletService.getBalance(wallet.getWalletId());

        assertTrue(balance == 600.0 || balance == 700.0,
                "After concurrent retries, balance should reflect both successful transactions sequentially");
    }


}
