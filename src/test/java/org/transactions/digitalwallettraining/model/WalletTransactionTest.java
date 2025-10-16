package org.transactions.digitalwallettraining.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.transactions.digitalwallettraining.entity.*;
import org.transactions.digitalwallettraining.repository.TransactionRepository;
import org.transactions.digitalwallettraining.repository.UserRepository;
import org.transactions.digitalwallettraining.repository.WalletRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional  // ensures DB is rolled back after each test
class RepositoryIntegrationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    // --------------------------------------------------------
    // USER REPOSITORY TESTS
    // --------------------------------------------------------
    @Test
    public void testUserRepositoryMethods() {
        // --- Create sample users ---
        UserEntity user1 = new UserEntity("Akshatha", "akshatha@example.com", 19);
        UserEntity user2 = new UserEntity("Akshay", "akshay@example.com", 24);
        UserEntity user3 = new UserEntity("John Doe", "john@example.com", 76);

        userRepository.saveAll(List.of(user1, user2, user3));

        // --- Test findByEmail ---
        Optional<UserEntity> fetched = userRepository.findByEmail("akshatha@example.com");
        assertTrue(fetched.isPresent());
        assertEquals("Akshatha", fetched.get().getName());

        // --- Test findByNameContainingIgnoreCase ---
        List<UserEntity> usersWithAk = userRepository.findByNameContainingIgnoreCase("ak");
        assertEquals(2, usersWithAk.size());

        // --- Test findByCreatedAtAfter ---
        LocalDateTime before = LocalDateTime.now().minusMinutes(1);
        List<UserEntity> recentUsers = userRepository.findByCreatedAtAfter(before);
        assertEquals(3, recentUsers.size());

        // --- Test findUsersWithHighBalanceWallets ---
        WalletEntity w1 = new WalletEntity();
        w1.setUser(user1);
        w1.setBalance(6000.0);

        WalletEntity w2 = new WalletEntity();
        w2.setUser(user2);
        w2.setBalance(4000.0);

        WalletEntity w3 = new WalletEntity();
        w3.setUser(user3);
        w3.setBalance(8000.0);

        walletRepository.saveAll(List.of(w1, w2, w3));

        List<UserEntity> richUsers = userRepository.findUsersWithHighBalanceWallets();
        assertEquals(2, richUsers.size()); // user1 and user3 expected
    }

    // --------------------------------------------------------
    // WALLET REPOSITORY TESTS
    // --------------------------------------------------------
    @Test
    public void testWalletRepositoryMethods() {
        UserEntity user = new UserEntity("WalletUser", "walletuser@example.com", 62);
        userRepository.save(user);

        WalletEntity w1 = new WalletEntity(); w1.setUser(user); w1.setBalance(6000.0);
        WalletEntity w2 = new WalletEntity(); w2.setUser(user); w2.setBalance(3000.0);
        WalletEntity w3 = new WalletEntity(); w3.setUser(user); w3.setBalance(7000.0);

        walletRepository.saveAll(List.of(w1, w2, w3));

        // --- findByUserId ---
        List<WalletEntity> wallets = walletRepository.findByUserId(user.getId());
        assertEquals(3, wallets.size());

        // --- findByBalanceGreaterThan ---
        List<WalletEntity> richWallets = walletRepository.findByBalanceGreaterThan(5000.0);
        assertEquals(2, richWallets.size());

        // --- findByCreatedAtBefore ---
        LocalDateTime future = LocalDateTime.now().plusMinutes(1);
        List<WalletEntity> walletsBefore = walletRepository.findByCreatedAtBefore(future);
        assertEquals(3, walletsBefore.size());

        // --- findWalletsAboveAverageBalance ---
        List<WalletEntity> aboveAvg = walletRepository.findWalletsAboveAverageBalance();
        assertEquals(2, aboveAvg.size());

        // --- findWalletsWithMoreThanXTransactions ---
        TransactionEntity t1 = new TransactionEntity(w1, TransactionType.CREDIT, 1000.0, "Test1");
        TransactionEntity t2 = new TransactionEntity(w1, TransactionType.DEBIT, 500.0, "Test2");
        TransactionEntity t3 = new TransactionEntity(w3, TransactionType.CREDIT, 2000.0, "Test3");

        transactionRepository.saveAll(List.of(t1, t2, t3));

        List<WalletEntity> walletsWithMoreTx = walletRepository.findWalletsWithMoreThanXTransactions(1);
        assertEquals(1, walletsWithMoreTx.size()); // only w1 has 2 transactions
    }

    // --------------------------------------------------------
    // TRANSACTION REPOSITORY TESTS
    // --------------------------------------------------------
    @Test
    public void testTransactionRepositoryMethods() {
        UserEntity user = new UserEntity("TxUser", "txuser@example.com", 17);
        userRepository.save(user);

        WalletEntity wallet = new WalletEntity();
        wallet.setUser(user);
        wallet.setBalance(5000.0);
        walletRepository.save(wallet);

        TransactionEntity t1 = new TransactionEntity(wallet, TransactionType.CREDIT, 2000.0, "Salary");
        TransactionEntity t2 = new TransactionEntity(wallet, TransactionType.DEBIT, 500.0, "Shopping");
        TransactionEntity t3 = new TransactionEntity(wallet, TransactionType.CREDIT, 1500.0, "Bonus");

        transactionRepository.saveAll(List.of(t1, t2, t3));

        // --- findByWalletId ---
        List<TransactionEntity> txByWallet = transactionRepository.findByWalletId(wallet.getId());
        assertEquals(3, txByWallet.size());

        // --- findByType ---
        List<TransactionEntity> credits = transactionRepository.findByType(TransactionType.CREDIT);
        assertEquals(2, credits.size());

        // --- findTransactionsGreaterThan ---
        List<TransactionEntity> txGreater = transactionRepository.findTransactionsGreaterThan(1600.0);
        assertEquals(1, txGreater.size());
        assertEquals(2000.0, txGreater.get(0).getAmount());

        // --- findByAmountBetween ---
        List<TransactionEntity> between = transactionRepository.findByAmountBetween(1000.0, 2000.0);
        assertEquals(2, between.size());

        // --- findByTransactionDateAfter ---
        LocalDateTime start = LocalDateTime.now().minusMinutes(1);
        List<TransactionEntity> recentTx = transactionRepository.findByTransactionDateAfter(start);
        assertEquals(3, recentTx.size());
    }

    // --------------------------------------------------------
    // CASCADE TEST
    // --------------------------------------------------------
    @Test
    public void testCascadeSaveUserWithWalletsAndTransactions() {
        UserEntity user = new UserEntity("Akshatha", "akshatha@example.com", 45);

        // Create wallets
        WalletEntity w1 = new WalletEntity();
        w1.setBalance(5000.0);
        WalletEntity w2 = new WalletEntity();
        w2.setBalance(2000.0);

        // Link wallets to user
        user.addWallet(w1);
        user.addWallet(w2);

        // Add transactions to wallets
        w1.addTransaction(new TransactionEntity(w1, TransactionType.CREDIT, 1000.0, "Salary"));
        w1.addTransaction(new TransactionEntity(w1, TransactionType.DEBIT, 500.0, "Shopping"));
        w2.addTransaction(new TransactionEntity(w2, TransactionType.CREDIT, 2000.0, "Freelance"));

        // Save user (should cascade wallets + transactions)
        userRepository.save(user);

        Optional<UserEntity> fetchedUser = userRepository.findById(user.getId());
        assertTrue(fetchedUser.isPresent());
        assertEquals(2, fetchedUser.get().getWallets().size());

        WalletEntity firstWallet = fetchedUser.get().getWallets().get(0);
        assertEquals(2, firstWallet.getTransactions().size());
    }
}
