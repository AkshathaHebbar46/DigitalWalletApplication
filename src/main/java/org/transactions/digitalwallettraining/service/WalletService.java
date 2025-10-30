package org.transactions.digitalwallettraining.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.transactions.digitalwallettraining.dto.*;
import org.transactions.digitalwallettraining.entity.*;
import org.transactions.digitalwallettraining.exception.MaxRetryExceededException;
import org.transactions.digitalwallettraining.repository.*;

import jakarta.persistence.OptimisticLockException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final WalletFreezeService walletFreezeService;

    private static final int MAX_RETRIES = 5;
    private static final long BASE_BACKOFF_MS = 100L;
    private static final double DAILY_LIMIT = 50000.0;
    private static final long FREEZE_DURATION_MINUTES = 2;

    public WalletService(WalletRepository walletRepository,
                         TransactionRepository transactionRepository,
                         UserRepository userRepository,
                         WalletFreezeService walletFreezeService) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.walletFreezeService = walletFreezeService;
    }

    // ✅ Create wallet
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public WalletResponseDTO createWallet(WalletRequestDTO request) {
        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        WalletEntity wallet = new WalletEntity(user, request.getBalance());
        walletRepository.save(wallet);

        log.info("✅ Wallet created for userId={} with balance={}", user.getId(), wallet.getBalance());
        return new WalletResponseDTO(wallet.getId(), user.getId(), wallet.getBalance());
    }

    @Transactional(readOnly = true)
    public Double getBalance(Long walletId) {
        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
        return wallet.getBalance();
    }

    private void validateWalletState(WalletEntity wallet) {
        wallet.resetDailyIfNewDay();

        if (Boolean.TRUE.equals(wallet.getFrozen()) && wallet.getFrozenAt() != null) {
            Duration diff = Duration.between(wallet.getFrozenAt(), LocalDateTime.now());
            if (diff.toMinutes() >= FREEZE_DURATION_MINUTES) {
                wallet.setFrozen(false);
                wallet.setFrozenAt(null);
                wallet.setDailySpent(0.0);
                walletRepository.save(wallet);
                log.info("✅ Wallet {} automatically unfrozen.", wallet.getId());
            }
        }

        if (Boolean.TRUE.equals(wallet.getFrozen())) {
            long secondsLeft = Math.max(0,
                    FREEZE_DURATION_MINUTES * 60 - Duration.between(wallet.getFrozenAt(), LocalDateTime.now()).toSeconds());
            throw new IllegalStateException("🚫 Wallet is frozen. Try again in " + secondsLeft + " seconds.");
        }
    }

    private void validateBalance(WalletEntity wallet, double amount) {
        if (wallet.getBalance() < amount) {
            throw new IllegalArgumentException("Insufficient balance.");
        }
    }

    private void validateAndTrackDailyLimit(WalletEntity wallet, double amount) {
        double total = wallet.getDailySpent() + amount;

        if (total > DAILY_LIMIT) {
            walletFreezeService.freezeWallet(wallet);
            throw new IllegalStateException(
                    "Daily transaction limit exceeded. Wallet frozen for " + FREEZE_DURATION_MINUTES + " minutes."
            );
        } else {
            wallet.setDailySpent(total);
            walletRepository.save(wallet);
        }
    }

    // ✅ Process transaction — NO LOCKING VERSION
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WalletTransactionResponseDTO processTransaction(Long walletId, WalletTransactionRequestDTO request) {
        int attempt = 0;

        while (true) {
            try {
                attempt++;

                WalletEntity wallet = walletRepository.findById(walletId)
                        .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

                validateWalletState(wallet);

                TransactionType type = TransactionType.valueOf(request.type().toUpperCase());
                double amount = request.amount();
                if (amount <= 0) throw new IllegalArgumentException("Amount must be positive.");

                if (type == TransactionType.DEBIT) {
                    validateBalance(wallet, amount);
                    validateAndTrackDailyLimit(wallet, amount);
                    wallet.setBalance(wallet.getBalance() - amount);
                } else {
                    wallet.setBalance(wallet.getBalance() + amount);
                }

                walletRepository.save(wallet);

                TransactionEntity txn = new TransactionEntity(wallet, type, amount, request.description());
                txn.setTransactionId(
                        request.transactionId() != null ? request.transactionId() : UUID.randomUUID().toString()
                );
                transactionRepository.save(txn);

                return new WalletTransactionResponseDTO(
                        txn.getTransactionId(),
                        txn.getAmount(),
                        type.name(),
                        txn.getTransactionDate(),
                        txn.getDescription()
                );

            } catch (OptimisticLockException | ObjectOptimisticLockingFailureException | CannotAcquireLockException ex) {
                if (attempt >= MAX_RETRIES) {
                    throw new MaxRetryExceededException("Max retry attempts exceeded for walletId=" + walletId, ex);
                }
                try {
                    Thread.sleep(BASE_BACKOFF_MS * (1L << (attempt - 1)));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WalletTransactionResponseDTO transferMoney(Long fromWalletId, Long toWalletId, Double amount) {
        if (Objects.equals(fromWalletId, toWalletId)) {
            throw new IllegalArgumentException("Cannot transfer to the same wallet.");
        }

        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive.");
        }

        WalletEntity fromWallet = walletRepository.findById(fromWalletId)
                .orElseThrow(() -> new IllegalArgumentException("Source wallet not found"));
        WalletEntity toWallet = walletRepository.findById(toWalletId)
                .orElseThrow(() -> new IllegalArgumentException("Destination wallet not found"));

        validateWalletState(fromWallet);
        validateWalletState(toWallet);

        validateBalance(fromWallet, amount);
        validateAndTrackDailyLimit(fromWallet, amount);

        fromWallet.setBalance(fromWallet.getBalance() - amount);
        toWallet.setBalance(toWallet.getBalance() + amount);

        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);

        String txnId = UUID.randomUUID().toString();

        TransactionEntity debitTxn = new TransactionEntity(fromWallet, TransactionType.DEBIT, amount,
                "Transfer to wallet " + toWalletId);
        debitTxn.setTransactionId(txnId + "-D");
        transactionRepository.save(debitTxn);

        TransactionEntity creditTxn = new TransactionEntity(toWallet, TransactionType.CREDIT, amount,
                "Transfer from wallet " + fromWalletId);
        creditTxn.setTransactionId(txnId + "-C");
        transactionRepository.save(creditTxn);

        return new WalletTransactionResponseDTO(
                debitTxn.getTransactionId(),
                debitTxn.getAmount(),
                debitTxn.getType().name(),
                debitTxn.getTransactionDate(),
                debitTxn.getDescription()
        );
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponseDTO> listTransactions(Long walletId) {
        return transactionRepository.findByWalletId(walletId).stream()
                .map(tx -> new WalletTransactionResponseDTO(
                        tx.getTransactionId(),
                        tx.getAmount(),
                        tx.getType().name(),
                        tx.getTransactionDate(),
                        tx.getDescription()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WalletResponseDTO> getAllWallets() {
        return walletRepository.findAll().stream()
                .map(wallet -> new WalletResponseDTO(wallet.getId(), wallet.getUser().getId(), wallet.getBalance()))
                .collect(Collectors.toList());
    }

    @Transactional
    public WalletResponseDTO getWalletDetails(Long walletId) {
        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
        validateWalletState(wallet);
        return new WalletResponseDTO(wallet.getId(), wallet.getUser().getId(), wallet.getBalance());
    }
}
