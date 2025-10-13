package org.transactions.digitalwallettraining.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.transactions.digitalwallettraining.entity.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class RepositoryIntegrationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    // ------------------ User Repository Tests ------------------
    @Test
    public void testUserRepositoryMethods() {
        // Create users
        UserEntity user1 = new UserEntity("Akshatha", "akshatha@example.com");
        UserEntity user2 = new UserEntity("Akshay", "akshay@example.com");
        UserEntity user3 = new UserEntity("John Doe", "john@example.com");

        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(user3);

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
        // Add wallets
        WalletEntity w1 = new WalletEntity(user1, 6000.0);
        WalletEntity w2 = new WalletEntity(user2, 4000.0);
        WalletEntity w3 = new WalletEntity(user3, 8000.0);

        walletRepository.save(w1);
        walletRepository.save(w2);
        walletRepository.save(w3);

        List<UserEntity> richUsers = userRepository.findUsersWithHighBalanceWallets();
        assertEquals(2, richUsers.size()); // user1 and user3
    }

    // ------------------ Wallet Repository Tests ------------------
    @Test
    public void testWalletRepositoryMethods() {
        // Create user
        UserEntity user = new UserEntity("WalletUser", "walletuser@example.com");
        userRepository.save(user);

        // Create wallets
        WalletEntity w1 = new WalletEntity(user, 6000.0);
        WalletEntity w2 = new WalletEntity(user, 3000.0);
        WalletEntity w3 = new WalletEntity(user, 7000.0);
        walletRepository.save(w1);
        walletRepository.save(w2);
        walletRepository.save(w3);

        // --- Test findByUserId ---
        List<WalletEntity> wallets = walletRepository.findByUserId(user.getId());
        assertEquals(3, wallets.size());

        // --- Test findByBalanceGreaterThan ---
        List<WalletEntity> richWallets = walletRepository.findByBalanceGreaterThan(5000.0);
        assertEquals(2, richWallets.size()); // 6000 + 7000

        // --- Test findByCreatedAtBefore ---
        LocalDateTime future = LocalDateTime.now().plusMinutes(1);
        List<WalletEntity> walletsBefore = walletRepository.findByCreatedAtBefore(future);
        assertEquals(3, walletsBefore.size());

        // --- Test findWalletsAboveAverageBalance ---
        List<WalletEntity> aboveAvg = walletRepository.findWalletsAboveAverageBalance();
        assertEquals(2, aboveAvg.size()); // 7000 is above average of (6000+3000+7000)/3 = 5333.33

        // --- Test findWalletsWithMoreThanXTransactions ---
        TransactionEntity t1 = new TransactionEntity(w1, TransactionEntity.TransactionType.CREDIT, 1000.0, "Test");
        TransactionEntity t2 = new TransactionEntity(w1, TransactionEntity.TransactionType.DEBIT, 500.0, "Test");
        TransactionEntity t3 = new TransactionEntity(w3, TransactionEntity.TransactionType.CREDIT, 2000.0, "Test");
        transactionRepository.save(t1);
        transactionRepository.save(t2);
        transactionRepository.save(t3);

        List<WalletEntity> walletsWithMoreTx = walletRepository.findWalletsWithMoreThanXTransactions(1);
        assertEquals(1, walletsWithMoreTx.size()); // only w1 has 2 transactions
    }

    // ------------------ Transaction Repository Tests ------------------
    @Test
    public void testTransactionRepositoryMethods() {
        // Create user & wallet
        UserEntity user = new UserEntity("TxUser", "txuser@example.com");
        userRepository.save(user);
        WalletEntity wallet = new WalletEntity(user, 5000.0);
        walletRepository.save(wallet);

        // Create transactions
        TransactionEntity t1 = new TransactionEntity(wallet, TransactionEntity.TransactionType.CREDIT, 2000.0, "Salary");
        TransactionEntity t2 = new TransactionEntity(wallet, TransactionEntity.TransactionType.DEBIT, 500.0, "Shopping");
        TransactionEntity t3 = new TransactionEntity(wallet, TransactionEntity.TransactionType.CREDIT, 1500.0, "Bonus");
        transactionRepository.save(t1);
        transactionRepository.save(t2);
        transactionRepository.save(t3);

        // --- Test findByWalletId ---
        List<TransactionEntity> txByWallet = transactionRepository.findByWalletId(wallet.getId());
        assertEquals(3, txByWallet.size());

        // --- Test findByType ---
        List<TransactionEntity> credits = transactionRepository.findByType(TransactionEntity.TransactionType.CREDIT);
        assertEquals(2, credits.size());

        // --- Test findTransactionsGreaterThan ---
        List<TransactionEntity> txGreater = transactionRepository.findTransactionsGreaterThan(1600.0);
        assertEquals(1, txGreater.size());
        assertEquals(2000.0, txGreater.get(0).getAmount());

        // --- Test findByAmountBetween ---
        List<TransactionEntity> between = transactionRepository.findByAmountBetween(1000.0, 2000.0);
        assertEquals(2, between.size());

        // --- Test findByTransactionDateAfter ---
        LocalDateTime start = LocalDateTime.now().minusMinutes(1);
        List<TransactionEntity> recentTx = transactionRepository.findByTransactionDateAfter(start);
        assertEquals(3, recentTx.size());
    }

    @Test
    public void testCascadeSaveUserWithWalletsAndTransactions() {
        UserEntity user = new UserEntity("Akshatha", "akshatha@example.com");

        WalletEntity w1 = new WalletEntity(null, 5000.0);
        WalletEntity w2 = new WalletEntity(null, 2000.0);

        // Add transactions
        w1.addTransaction(new TransactionEntity(null, TransactionEntity.TransactionType.CREDIT, 1000.0, "Salary"));
        w1.addTransaction(new TransactionEntity(null, TransactionEntity.TransactionType.DEBIT, 500.0, "Shopping"));
        w2.addTransaction(new TransactionEntity(null, TransactionEntity.TransactionType.CREDIT, 2000.0, "Freelance"));

        // Add wallets to user
        user.addWallet(w1);
        user.addWallet(w2);

        // Save user → cascades to wallets and transactions
        userRepository.save(user);

        // Fetch back to verify
        Optional<UserEntity> fetchedUser = userRepository.findById(user.getId());
        assertTrue(fetchedUser.isPresent());
        assertEquals(2, fetchedUser.get().getWallets().size());

        WalletEntity firstWallet = fetchedUser.get().getWallets().get(0);
        assertEquals(2, firstWallet.getTransactions().size());
    }

}
