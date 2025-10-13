package org.transactions.digitalwallettraining.repository;

import org.transactions.digitalwallettraining.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    // Find transactions by wallet
    List<TransactionEntity> findByWalletId(Long walletId);

    // Custom JPQL query example: find all transactions above a certain amount
    @Query("SELECT t FROM TransactionEntity t WHERE t.amount > :amount")
    List<TransactionEntity> findTransactionsGreaterThan(Double amount);

    // Find transactions by type (CREDIT or DEBIT)
    List<TransactionEntity> findByType(TransactionEntity.TransactionType type);

    // Find transactions with amount between two values
    List<TransactionEntity> findByAmountBetween(Double min, Double max);

    // Find transactions after a certain date
    List<TransactionEntity> findByTransactionDateAfter(java.time.LocalDateTime date);
}
