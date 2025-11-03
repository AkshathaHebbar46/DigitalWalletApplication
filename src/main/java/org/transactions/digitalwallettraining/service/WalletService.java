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
import org.transactions.digitalwallettraining.repository.*;

import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;

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
        log.info("✅ Wallet created for userId={} with initial balance ₹{}", user.getId(), wallet.getBalance());
        return new WalletResponseDTO(wallet.getId(), user.getId(), wallet.getBalance());
    }

    // ✅ Get balance
    @Transactional(readOnly = true)
    public Double getBalance(Long walletId) {
        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
        return wallet.getBalance();
    }

    // ✅ Validate wallet state (check freeze/unfreeze)
    private void validateWalletState(WalletEntity wallet) {
        wallet.resetDailyIfNewDay();

        if (Boolean.TRUE.equals(wallet.getFrozen()) && wallet.getFrozenAt() != null) {
            long elapsed = Duration.between(wallet.getFrozenAt(), LocalDateTime.now()).toMinutes();

            if (elapsed >= FREEZE_DURATION_MINUTES) {
                walletFreezeService.unfreezeWallet(wallet); // ✅ correct
                log.info("🧊 Wallet {} unfrozen after {} minutes", wallet.getId(), FREEZE_DURATION_MINUTES);
            }
            else {
                long secondsLeft = FREEZE_DURATION_MINUTES * 60 -
                        Duration.between(wallet.getFrozenAt(), LocalDateTime.now()).toSeconds();
                log.warn("⏳ Wallet {} still frozen. {} seconds remaining.", wallet.getId(), secondsLeft);
                throw new IllegalStateException("🚫 Wallet is frozen. Try again in " + secondsLeft + " seconds.");
            }
        }
    }

    private void validateBalance(WalletEntity wallet, double amount) {
        if (wallet.getBalance() < amount) {
            log.error("❌ Insufficient balance in wallet {}. Available: ₹{}, Required: ₹{}",
                    wallet.getId(), wallet.getBalance(), amount);
            throw new IllegalArgumentException("Insufficient balance.");
        }
    }

    // ✅ Track daily spent and freeze only when limit reached exactly
    private void validateAndTrackDailyLimit(WalletEntity wallet, double amount) {
        double newTotal = wallet.getDailySpent() + amount;

        if (newTotal > DAILY_LIMIT) {
            double available = DAILY_LIMIT - wallet.getDailySpent();
            log.warn("🚫 Wallet {}: Daily limit exceeded. Attempted ₹{}, Available ₹{}",
                    wallet.getId(), amount, available);
            throw new IllegalStateException("🚫 Daily limit exceeded. Available limit: ₹" + available);
        }

        wallet.setDailySpent(newTotal);
        walletRepository.saveAndFlush(wallet);
        log.info("💸 Wallet {} daily spent updated: ₹{}/₹{}", wallet.getId(), newTotal, DAILY_LIMIT);

        // ✅ Post-commit freeze trigger
        if (wallet.getDailySpent() >= DAILY_LIMIT) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("🧭 Transaction committed. Freezing wallet {} post-commit.", wallet.getId());
                    walletFreezeService.freezeWallet(wallet);
                }
            });
        }
    }

    // ✅ Process transaction
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WalletTransactionResponseDTO processTransaction(Long walletId, WalletTransactionRequestDTO request) {
        int attempt = 0;

        while (attempt < MAX_RETRIES) {
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

                log.info("✅ Transaction {} completed successfully for wallet {} (amount ₹{})",
                        txn.getTransactionId(), walletId, amount);

                return new WalletTransactionResponseDTO(
                        txn.getTransactionId(),
                        txn.getAmount(),
                        type.name(),
                        txn.getTransactionDate(),
                        txn.getDescription()
                );

            } catch (ObjectOptimisticLockingFailureException | CannotAcquireLockException ex) {
                log.warn("⚠️ Wallet {} busy (attempt {}/{}). Retrying...", walletId, attempt, MAX_RETRIES);
                try {
                    Thread.sleep(BASE_BACKOFF_MS * (1L << (attempt - 1)));
                } catch (InterruptedException ignored) { }
            } catch (IllegalStateException | IllegalArgumentException ex) {
                log.error("🚫 Transaction rejected for wallet {}: {}", walletId, ex.getMessage());
                throw ex;
            } catch (Exception ex) {
                log.error("❌ Unexpected error processing wallet {}: {}", walletId, ex.getMessage());
                throw ex;
            }
        }

        // ⚠️ After MAX_RETRIES
        log.error("🚫 Could not process wallet {} after {} retries. Please try again later.", walletId, MAX_RETRIES);

        // Instead of throwing an error → return user-friendly message
        throw new IllegalStateException("Please try again later. Wallet is busy processing another transaction.");
    }

    // Transfer money
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WalletTransactionResponseDTO transferMoney(Long fromWalletId, Long toWalletId, Double amount) {
        if (Objects.equals(fromWalletId, toWalletId))
            throw new IllegalArgumentException("Cannot transfer to the same wallet.");
        if (amount == null || amount <= 0)
            throw new IllegalArgumentException("Transfer amount must be positive.");

        WalletEntity from = walletRepository.findById(fromWalletId)
                .orElseThrow(() -> new IllegalArgumentException("Source wallet not found"));
        WalletEntity to = walletRepository.findById(toWalletId)
                .orElseThrow(() -> new IllegalArgumentException("Destination wallet not found"));

        validateWalletState(from);
        validateWalletState(to);
        validateBalance(from, amount);
        validateAndTrackDailyLimit(from, amount);

        from.setBalance(from.getBalance() - amount);
        to.setBalance(to.getBalance() + amount);

        walletRepository.save(from);
        walletRepository.save(to);

        String txnId = UUID.randomUUID().toString();
        TransactionEntity debit = new TransactionEntity(from, TransactionType.DEBIT, amount,
                "Transfer to wallet " + toWalletId);
        debit.setTransactionId(txnId + "-D");
        transactionRepository.save(debit);

        TransactionEntity credit = new TransactionEntity(to, TransactionType.CREDIT, amount,
                "Transfer from wallet " + fromWalletId);
        credit.setTransactionId(txnId + "-C");
        transactionRepository.save(credit);

        log.info("🔁 Transfer completed: ₹{} from wallet {} → wallet {}",
                amount, fromWalletId, toWalletId);

        return new WalletTransactionResponseDTO(
                debit.getTransactionId(), debit.getAmount(),
                debit.getType().name(), debit.getTransactionDate(), debit.getDescription());
    }

    // ✅ View transactions
    @Transactional(readOnly = true)
    public List<WalletTransactionResponseDTO> listTransactions(Long walletId) {
        return transactionRepository.findByWalletId(walletId).stream()
                .map(tx -> new WalletTransactionResponseDTO(
                        tx.getTransactionId(), tx.getAmount(), tx.getType().name(),
                        tx.getTransactionDate(), tx.getDescription()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WalletResponseDTO> getAllWallets() {
        return walletRepository.findAll().stream()
                .map(w -> new WalletResponseDTO(w.getId(), w.getUser().getId(), w.getBalance()))
                .collect(Collectors.toList());
    }

    @Transactional
    public WalletResponseDTO getWalletDetails(Long walletId) {
        WalletEntity w = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
        validateWalletState(w);
        return new WalletResponseDTO(w.getId(), w.getUser().getId(), w.getBalance());
    }
}
