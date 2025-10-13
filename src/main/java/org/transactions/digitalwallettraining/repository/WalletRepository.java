package org.transactions.digitalwallettraining.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.transactions.digitalwallettraining.entity.WalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WalletRepository extends JpaRepository<WalletEntity, Long> {

    // Find all wallets belonging to a specific user
    List<WalletEntity> findByUserId(Long userId);

    // Find wallets with balance greater than a certain amount
    List<WalletEntity> findByBalanceGreaterThan(Double amount);

    // Find wallets created before a certain date
    List<WalletEntity> findByCreatedAtBefore(java.time.LocalDateTime date);

    @Query("SELECT w FROM WalletEntity w WHERE w.balance > (SELECT AVG(w2.balance) FROM WalletEntity w2)")
    List<WalletEntity> findWalletsAboveAverageBalance();

    // Find wallets with more than X transactions
    @Query("SELECT w FROM WalletEntity w WHERE SIZE(w.transactions) > :count")
    List<WalletEntity> findWalletsWithMoreThanXTransactions(@Param("count") int count);
}
