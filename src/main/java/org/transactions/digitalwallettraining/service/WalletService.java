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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    private static final int MAX_RETRIES = 5;
    private static final long BASE_BACKOFF_MS = 100L;

    public WalletService(WalletRepository walletRepository,
                         TransactionRepository transactionRepository,
                         UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    // 🔹 Create Wallet
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public WalletResponseDTO createWallet(WalletRequestDTO request) {
        log.info("Creating wallet for userId={} with initial balance={}", request.getUserId(), request.getBalance());

        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> {
                    log.error("User not found with id={}", request.getUserId());
                    return new IllegalArgumentException("User not found");
                });

        WalletEntity wallet = new WalletEntity(user, request.getBalance());
        walletRepository.save(wallet);

        log.info("Wallet created successfully with walletId={} for userId={}", wallet.getId(), user.getId());
        return new WalletResponseDTO(wallet.getId(), user.getId(), wallet.getBalance());
    }

    // 🔹 Get Wallet Balance
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Double getBalance(Long walletId) {
        log.debug("Fetching balance for walletId={}", walletId);

        WalletEntity wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> {
                    log.error("Wallet not found with id={}", walletId);
                    return new IllegalArgumentException("Wallet not found");
                });

        log.info("Wallet balance for walletId={} is {}", walletId, wallet.getBalance());
        return wallet.getBalance();
    }

    // 🔹 Process Credit/Debit Transaction with Retry + Logging
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public WalletTransactionResponseDTO processTransaction(Long walletId, WalletTransactionRequestDTO request) {
        int attempt = 0;
        log.info("Starting {} transaction for walletId={} with amount={} and txnId={}",
                request.type(), walletId, request.amount(), request.transactionId());

        // 🧩 Step 1: Check if transactionId already exists (idempotency)
        if (request.transactionId() != null) {
            TransactionEntity existing = transactionRepository.findByTransactionId(request.transactionId());
            if (existing != null) {
                log.info("Duplicate transaction detected (idempotent) — returning existing record for txnId={}", request.transactionId());
                return new WalletTransactionResponseDTO(
                        existing.getTransactionId(),
                        existing.getAmount(),
                        existing.getType().name(),
                        existing.getTransactionDate(),
                        existing.getDescription()
                );
            }
        }

        while (true) {
            try {
                attempt++;
                WalletEntity wallet = walletRepository.findById(walletId)
                        .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

                TransactionType type = TransactionType.valueOf(request.type().toUpperCase());
                double amount = request.amount();

                if (amount <= 0) {
                    throw new IllegalArgumentException("Transaction amount must be positive");
                }
                if (type == TransactionType.DEBIT && wallet.getBalance() < amount) {
                    throw new IllegalArgumentException("Insufficient balance");
                }

                double newBalance = (type == TransactionType.CREDIT)
                        ? wallet.getBalance() + amount
                        : wallet.getBalance() - amount;

                wallet.setBalance(newBalance);

                // 🧩 Step 2: Generate transactionId if not provided
                String txnId = request.transactionId() != null ? request.transactionId() : UUID.randomUUID().toString();

                TransactionEntity transaction = new TransactionEntity(
                        wallet,
                        type,
                        amount,
                        request.description()
                );
                transaction.setTransactionId(txnId);

                transactionRepository.save(transaction);
                walletRepository.saveAndFlush(wallet);

                log.info("{} transaction successful for walletId={} | txnId={} | newBalance={}",
                        type, walletId, txnId, wallet.getBalance());

                return new WalletTransactionResponseDTO(
                        transaction.getTransactionId(),
                        transaction.getAmount(),
                        type.name(),
                        transaction.getTransactionDate(),
                        transaction.getDescription()
                );

            } catch (OptimisticLockException | ObjectOptimisticLockingFailureException | CannotAcquireLockException ex) {
                log.warn("⚠️ Concurrency conflict on walletId={} (attempt {}): {}", walletId, attempt, ex.getMessage());
                if (attempt >= MAX_RETRIES) {
                    throw new MaxRetryExceededException(
                            "Max retry attempts exceeded due to concurrent modification. Please retry the request.", ex
                    );
                }

                try {
                    long sleepMs = BASE_BACKOFF_MS * (1L << (attempt - 1));
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Transaction interrupted while retrying", ie);
                }
            }
        }
    }


    // 🔹 List Transactions
    @Transactional(readOnly = true)
    public List<WalletTransactionResponseDTO> listTransactions(Long walletId) {
        log.debug("Fetching transaction history for walletId={}", walletId);

        List<TransactionEntity> transactions = transactionRepository.findByWalletId(walletId);
        log.info("Found {} transactions for walletId={}", transactions.size(), walletId);

        return transactions.stream()
                .map(tx -> new WalletTransactionResponseDTO(
                        tx.getTransactionId(),
                        tx.getAmount(),
                        tx.getType().name(),
                        tx.getTransactionDate(),
                        tx.getDescription()
                ))
                .collect(Collectors.toList());
    }

    // 🔹 Transfer Money Between Wallets
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public WalletTransactionResponseDTO transferMoney(Long fromWalletId,
                                                      Long toWalletId,
                                                      WalletTransactionRequestDTO request) {
        log.info("Initiating transfer: fromWalletId={} → toWalletId={} | amount={}",
                fromWalletId, toWalletId, request.amount());

        if (fromWalletId.equals(toWalletId)) {
            log.error("Transfer failed: source and target wallets are the same (walletId={})", fromWalletId);
            throw new IllegalArgumentException("Cannot transfer to the same wallet");
        }

        double amount = request.amount();
        if (amount <= 0) {
            log.error("Invalid transfer amount={} from walletId={}", amount, fromWalletId);
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        Long firstId = Math.min(fromWalletId, toWalletId);
        Long secondId = Math.max(fromWalletId, toWalletId);

        WalletEntity first = walletRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + firstId));
        WalletEntity second = walletRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + secondId));

        WalletEntity source = fromWalletId.equals(firstId) ? first : second;
        WalletEntity target = toWalletId.equals(firstId) ? first : second;

        if (source.getBalance() < amount) {
            log.warn("Transfer failed: insufficient funds in walletId={} (balance={}, amount={})",
                    source.getId(), source.getBalance(), amount);
            throw new IllegalArgumentException("Insufficient balance in source wallet");
        }

        source.setBalance(source.getBalance() - amount);
        target.setBalance(target.getBalance() + amount);

        TransactionEntity debitTxn = new TransactionEntity(source, TransactionType.DEBIT, amount,
                "Transfer to wallet " + toWalletId);
        debitTxn.setTransactionId(request.transactionId() != null ? request.transactionId() : UUID.randomUUID().toString());

        TransactionEntity creditTxn = new TransactionEntity(target, TransactionType.CREDIT, amount,
                "Transfer from wallet " + fromWalletId);
        creditTxn.setTransactionId(UUID.randomUUID().toString());

        transactionRepository.save(debitTxn);
        transactionRepository.save(creditTxn);
        walletRepository.saveAndFlush(source);
        walletRepository.saveAndFlush(target);

        log.info("✅ Transfer completed successfully: {} → {} | amount={} | sourceBalance={} | targetBalance={}",
                fromWalletId, toWalletId, amount, source.getBalance(), target.getBalance());

        return new WalletTransactionResponseDTO(
                creditTxn.getTransactionId(),
                creditTxn.getAmount(),
                creditTxn.getType().name(),
                creditTxn.getTransactionDate(),
                creditTxn.getDescription()
        );
    }
}
