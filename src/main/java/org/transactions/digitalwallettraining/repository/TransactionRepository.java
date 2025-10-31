package org.transactions.digitalwallettraining.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.repository.query.Param;
import org.transactions.digitalwallettraining.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.transactions.digitalwallettraining.entity.TransactionType;
import org.transactions.digitalwallettraining.entity.WalletEntity;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    // Find transactions by wallet
    List<TransactionEntity> findByWalletId(Long walletId);

    // Custom JPQL query example: find all transactions above a certain amount
    @Query("SELECT t FROM TransactionEntity t WHERE t.amount > :amount")
    List<TransactionEntity> findTransactionsGreaterThan(Double amount);

    // Find transactions by type (CREDIT or DEBIT)
    List<TransactionEntity> findByType(TransactionType type);

    // Find transactions with amount between two values
    List<TransactionEntity> findByAmountBetween(Double min, Double max);

    // Find transactions after a certain date
    List<TransactionEntity> findByTransactionDateAfter(java.time.LocalDateTime date);

    Optional<TransactionEntity> findByTransactionId(String transactionId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionEntity t WHERE t.wallet.id = :walletId AND t.type = 'DEBIT' AND t.transactionDate BETWEEN :start AND :end")
    double sumDebitsByWalletAndDate(@Param("walletId") Long walletId,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    @Query("""
        SELECT t FROM TransactionEntity t 
        WHERE t.wallet.id = :walletId
        AND (:type IS NULL OR t.type = :type)
        AND (:startDate IS NULL OR t.transactionDate >= :startDate)
        AND (:endDate IS NULL OR t.transactionDate <= :endDate)
        ORDER BY t.transactionDate DESC
    """)
    Page<TransactionEntity> findFilteredTransactions(
            Long walletId,
            org.transactions.digitalwallettraining.entity.TransactionType type,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );


}
