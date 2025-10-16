package org.transactions.digitalwallettraining.repository;

import org.junit.jupiter.api.BeforeEach;
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

    private UserEntity user1, user2, user3;
    private WalletEntity w1, w2, w3;

    @BeforeEach
    public void setup() {
        // Clear all repositories
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();

        // Create users with age
        user1 = new UserEntity("Akshatha", "akshatha@example.com", 25);
        user2 = new UserEntity("Akshay", "akshay@example.com", 30);
        user3 = new UserEntity("John Doe", "john@example.com", 22);
        userRepository.saveAll(List.of(user1, user2, user3));

        // Create wallets
        w1 = new WalletEntity(user1, 6000.0);
        w2 = new WalletEntity(user2, 4000.0);
        w3 = new WalletEntity(user3, 8000.0);
        walletRepository.saveAll(List.of(w1, w2, w3));

        // Create transactions
        TransactionEntity t1 = new TransactionEntity(w1, TransactionType.CREDIT, 1000.0, "Salary");
        TransactionEntity t2 = new TransactionEntity(w1, TransactionType.DEBIT, 500.0, "Shopping");
        TransactionEntity t3 = new TransactionEntity(w2, TransactionType.CREDIT, 4000.0, "Initial deposit");
        TransactionEntity t4 = new TransactionEntity(w3, TransactionType.CREDIT, 8000.0, "Initial deposit");
        transactionRepository.saveAll(List.of(t1, t2, t3, t4));
    }

    // ------------------ User Repository Tests ------------------
    @Test
    public void testUserRepositoryMethods() {
        // --- findByEmail ---
        Optional<UserEntity> fetched = userRepository.findByEmail("akshatha@example.com");
        assertTrue(fetched.isPresent());
        assertEquals("Akshatha", fetched.get().getName());
        assertEquals(25, fetched.get().getAge());

        // --- findByNameContainingIgnoreCase ---
        List<UserEntity> usersWithAk = userRepository.findByNameContainingIgnoreCase("ak");
        assertEquals(2, usersWithAk.size()); // Akshatha + Akshay

        // --- findByCreatedAtAfter ---
        LocalDateTime before = LocalDateTime.now().minusMinutes(1);
        List<UserEntity> recentUsers = userRepository.findByCreatedAtAfter(before);
        assertEquals(3, recentUsers.size());

        // --- findUsersWithHighBalanceWallets ---
        List<UserEntity> richUsers = userRepository.findUsersWithHighBalanceWallets();
        assertEquals(2, richUsers.size()); // w1 and w3
    }

    // ------------------ Wallet Repository Tests ------------------
    @Test
    public void testWalletRepositoryMethods() {
        List<WalletEntity> wallets = walletRepository.findByUserId(user1.getId());
        assertEquals(1, wallets.size());

        List<WalletEntity> richWallets = walletRepository.findByBalanceGreaterThan(5000.0);
        assertEquals(2, richWallets.size()); // w1 and w3

        LocalDateTime future = LocalDateTime.now().plusMinutes(1);
        List<WalletEntity> walletsBefore = walletRepository.findByCreatedAtBefore(future);
        assertEquals(3, walletsBefore.size());

        List<WalletEntity> aboveAvg = walletRepository.findWalletsAboveAverageBalance();
        assertEquals(1, aboveAvg.size()); // w1=6000, w3=8000, avg=6000

        List<WalletEntity> walletsWithMoreTx = walletRepository.findWalletsWithMoreThanXTransactions(1);
        assertEquals(1, walletsWithMoreTx.size()); // only w1 has 2 transactions
    }

    // ------------------ Transaction Repository Tests ------------------
    @Test
    public void testTransactionRepositoryMethods() {
        List<TransactionEntity> txByWallet = transactionRepository.findByWalletId(w1.getId());
        assertEquals(2, txByWallet.size());

        List<TransactionEntity> credits = transactionRepository.findByType(TransactionType.CREDIT);
        assertEquals(3, credits.size());

        List<TransactionEntity> txGreater = transactionRepository.findTransactionsGreaterThan(1600.0);
        assertEquals(2, txGreater.size()); // 4000 + 8000
        assertTrue(txGreater.stream().allMatch(tx -> tx.getAmount() > 1600));

        List<TransactionEntity> between = transactionRepository.findByAmountBetween(500.0, 5000.0);
        assertEquals(3, between.size()); // 1000, 500, 4000

        LocalDateTime start = LocalDateTime.now().minusMinutes(1);
        List<TransactionEntity> recentTx = transactionRepository.findByTransactionDateAfter(start);
        assertEquals(4, recentTx.size());
    }

    // ------------------ Cascade Test ------------------
    @Test
    public void testCascadeSaveUserWithWalletsAndTransactions() {
        UserEntity user = new UserEntity("CascadeUser", "cascade@example.com", 28);

        WalletEntity walletA = new WalletEntity();
        walletA.setBalance(5000.0);

        WalletEntity walletB = new WalletEntity();
        walletB.setBalance(2000.0);

        user.addWallet(walletA);
        user.addWallet(walletB);

        walletA.addTransaction(new TransactionEntity(walletA, TransactionType.CREDIT, 1000.0, "Salary"));
        walletA.addTransaction(new TransactionEntity(walletA, TransactionType.DEBIT, 500.0, "Shopping"));
        walletB.addTransaction(new TransactionEntity(walletB, TransactionType.CREDIT, 2000.0, "Freelance"));

        userRepository.save(user);

        Optional<UserEntity> fetchedUser = userRepository.findById(user.getId());
        assertTrue(fetchedUser.isPresent());
        assertEquals(2, fetchedUser.get().getWallets().size());

        WalletEntity firstWallet = fetchedUser.get().getWallets().get(0);
        assertEquals(2, firstWallet.getTransactions().size());
    }

    // ------------------ Boundary & Edge Cases ------------------
    @Test
    public void testBoundaryConditions() {
        WalletEntity zeroWallet = new WalletEntity(user1, 0.0);
        walletRepository.save(zeroWallet);
        assertEquals(0.0, walletRepository.findById(zeroWallet.getId()).get().getBalance());

        WalletEntity negativeWallet = new WalletEntity();
        negativeWallet.setUser(user1);
        assertThrows(IllegalArgumentException.class, () -> negativeWallet.setBalance(-100.0));

        assertThrows(IllegalArgumentException.class, () -> {
            new TransactionEntity(zeroWallet, TransactionType.CREDIT, 0.0, "Zero amount not allowed");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new TransactionEntity(zeroWallet, TransactionType.DEBIT, -50.0, "Negative amount not allowed");
        });

        TransactionEntity validTx = new TransactionEntity(zeroWallet, TransactionType.CREDIT, 100.0, "Valid Transaction");
        transactionRepository.save(validTx);
        assertTrue(transactionRepository.findById(validTx.getId()).isPresent());
    }
}
