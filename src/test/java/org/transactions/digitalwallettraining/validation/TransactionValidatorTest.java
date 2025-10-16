package org.transactions.digitalwallettraining.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.transactions.digitalwallettraining.entity.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class TransactionValidatorTest {

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
        Optional<UserEntity> fetched = userRepository.findByEmail("akshatha@example.com");
        assertTrue(fetched.isPresent());
        assertEquals("Akshatha", fetched.get().getName());
        assertEquals(25, fetched.get().getAge());

        List<UserEntity> usersWithAk = userRepository.findByNameContainingIgnoreCase("ak");
        assertEquals(2, usersWithAk.size());

        List<UserEntity> richUsers = userRepository.findUsersWithHighBalanceWallets();
        assertEquals(2, richUsers.size()); // w1 and w3
    }

    // ------------------ Wallet Repository Tests ------------------
    @Test
    public void testWalletRepositoryMethods() {
        List<WalletEntity> wallets = walletRepository.findByUserId(user1.getId());
        assertEquals(1, wallets.size());

        List<WalletEntity> richWallets = walletRepository.findByBalanceGreaterThan(5000.0);
        assertEquals(2, richWallets.size());

        List<WalletEntity> aboveAvg = walletRepository.findWalletsAboveAverageBalance();
        assertEquals(2, aboveAvg.size());

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
        assertEquals(2, txGreater.size());

        List<TransactionEntity> between = transactionRepository.findByAmountBetween(500.0, 5000.0);
        assertEquals(3, between.size());
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

        // Transaction amount zero should be allowed
        TransactionEntity zeroTx = new TransactionEntity(zeroWallet, TransactionType.CREDIT, 0.0, "Zero Tx");
        transactionRepository.save(zeroTx);
        assertEquals(0.0, transactionRepository.findById(zeroTx.getId()).get().getAmount());
    }
}
