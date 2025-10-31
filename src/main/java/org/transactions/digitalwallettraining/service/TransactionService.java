package org.transactions.digitalwallettraining.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.transactions.digitalwallettraining.dto.WalletTransactionResponseDTO;
import org.transactions.digitalwallettraining.entity.TransactionEntity;
import org.transactions.digitalwallettraining.entity.TransactionType;
import org.transactions.digitalwallettraining.repository.TransactionRepository;

import java.time.LocalDateTime;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Page<WalletTransactionResponseDTO> getFilteredTransactions(
            Long walletId,
            TransactionType type,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        Page<TransactionEntity> transactions = transactionRepository.findFilteredTransactions(
                walletId, type, startDate, endDate, pageable
        );

        // Convert TransactionEntity → WalletTransactionResponseDTO
        return transactions.map(txn -> new WalletTransactionResponseDTO(
                txn.getTransactionId(),
                txn.getAmount(),
                txn.getType().name(),
                txn.getTransactionDate(),
                txn.getDescription()

        ));

    }
}
