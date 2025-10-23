package org.transactions.digitalwallettraining.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.transactions.digitalwallettraining.dto.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final Map<Long, WalletResponseDTO> wallets = new HashMap<>();
    private final Map<Long, List<WalletTransactionResponseDTO>> transactions = new HashMap<>();
    private Long walletCounter = 1L;

    private static final double MAX_TRANSACTION_AMOUNT = 100_000; // 1 lakh
    private static final double MAX_DAILY_DEBIT = 100_000; // 1 lakh

    // Create a wallet
    public WalletResponseDTO createWallet(WalletRequestDTO request) {
        Long walletId = walletCounter++;
        log.info("Creating wallet for userId: {}, initial balance: {}", request.getUserId(), request.getBalance());

        WalletResponseDTO wallet = new WalletResponseDTO(walletId, request.getUserId(), request.getBalance());
        wallets.put(walletId, wallet);
        transactions.put(walletId, new ArrayList<>());

        log.debug("Wallet created successfully: {}", wallet);
        return wallet;
    }

    // Get wallet balance
    public Double getBalance(Long walletId) {
        log.info("Fetching balance for walletId: {}", walletId);
        WalletResponseDTO wallet = wallets.get(walletId);

        if (wallet == null) {
            log.error("Wallet not found: {}", walletId);
            throw new NoSuchElementException("Wallet not found");
        }

        log.debug("Wallet balance retrieved: {}", wallet.getBalance());
        return wallet.getBalance();
    }

    // Process a transaction
    public WalletTransactionResponseDTO processTransaction(Long walletId, WalletTransactionRequestDTO request) {
        WalletResponseDTO wallet = wallets.get(walletId);
        if (wallet == null) {
            log.error("Wallet not found: {}", walletId);
            throw new NoSuchElementException("Wallet not found");
        }

        Double amount = request.amount();
        String type = request.type().toUpperCase();

        log.info("Processing {} transaction for walletId: {} with amount: {}", type, walletId, amount);

        // Validate per-transaction amount
        if (amount <= 0 || amount > MAX_TRANSACTION_AMOUNT) {
            log.warn("Transaction amount {} is invalid for walletId: {}", amount, walletId);
            throw new IllegalArgumentException("Transaction amount must be between 0 and 1 lakh");
        }

        if ("DEBIT".equals(type)) {
            // Calculate total debit for today
            double dailyDebitTotal = transactions.get(walletId).stream()
                    .filter(tx -> "DEBIT".equalsIgnoreCase(tx.type()) &&
                            tx.timestamp().toLocalDate().equals(LocalDate.now()))
                    .mapToDouble(WalletTransactionResponseDTO::amount)
                    .sum();

            if (dailyDebitTotal + amount > MAX_DAILY_DEBIT) {
                log.warn("Daily debit limit exceeded for walletId: {} (current total={}, attempted debit={})",
                        walletId, dailyDebitTotal, amount);
                throw new IllegalArgumentException("Daily debit limit of 1 lakh exceeded for walletId: " + walletId);
            }

            if (wallet.getBalance() < amount) {
                log.warn("Insufficient balance for walletId: {} (balance={}, attempted debit={})",
                        walletId, wallet.getBalance(), amount);
                throw new IllegalArgumentException("Insufficient wallet balance");
            }

            wallet.setBalance(wallet.getBalance() - amount);
            log.debug("Wallet debited: new balance = {}", wallet.getBalance());

        } else if ("CREDIT".equals(type)) {
            wallet.setBalance(wallet.getBalance() + amount);
            log.debug("Wallet credited: new balance = {}", wallet.getBalance());
        } else {
            log.error("Invalid transaction type: {}", type);
            throw new IllegalArgumentException("Transaction type must be CREDIT or DEBIT");
        }

        // Create transaction with timestamp
        WalletTransactionResponseDTO response = new WalletTransactionResponseDTO(
                request.transactionId(),
                amount,
                type,
                LocalDateTime.now(), // timestamp required for daily limit
                request.description()
        );

        transactions.get(walletId).add(response);
        log.info("Transaction completed successfully for walletId: {}, transactionId: {}", walletId, request.transactionId());

        return response;
    }

    // List transactions
    public List<WalletTransactionResponseDTO> listTransactions(Long walletId) {
        log.info("Listing transactions for walletId: {}", walletId);
        if (!transactions.containsKey(walletId)) {
            log.error("Wallet not found: {}", walletId);
            throw new NoSuchElementException("Wallet not found");
        }

        List<WalletTransactionResponseDTO> txList = transactions.get(walletId);
        log.debug("Total transactions found: {}", txList.size());
        return txList;
    }

    public WalletTransactionResponseDTO transferMoney(Long fromWalletId, Long toWalletId, WalletTransactionRequestDTO request) {
        log.info("Transferring {} from wallet {} to wallet {}", request.amount(), fromWalletId, toWalletId);

        WalletResponseDTO fromWallet = wallets.get(fromWalletId);
        WalletResponseDTO toWallet = wallets.get(toWalletId);

        if (fromWallet == null) throw new NoSuchElementException("Source wallet not found");
        if (toWallet == null) throw new NoSuchElementException("Destination wallet not found");

        Double amount = request.amount();

        // Validate amount
        if (amount <= 0 || amount > MAX_TRANSACTION_AMOUNT) {
            throw new IllegalArgumentException("Transaction amount must be between 0 and 1 lakh");
        }

        // Check daily debit limit for sender
        double dailyDebitTotal = transactions.get(fromWalletId).stream()
                .filter(tx -> "DEBIT".equalsIgnoreCase(tx.type()) &&
                        tx.timestamp().toLocalDate().equals(LocalDate.now()))
                .mapToDouble(WalletTransactionResponseDTO::amount)
                .sum();

        if (dailyDebitTotal + amount > MAX_DAILY_DEBIT) {
            throw new IllegalArgumentException("Daily debit limit of 1 lakh exceeded for walletId: " + fromWalletId);
        }

        // Check sender balance
        if (fromWallet.getBalance() < amount) {
            throw new IllegalArgumentException("Insufficient balance in source wallet");
        }

        // Perform debit from source
        fromWallet.setBalance(fromWallet.getBalance() - amount);
        WalletTransactionResponseDTO debitTx = new WalletTransactionResponseDTO(
                request.transactionId() + "-DEBIT",
                amount,
                "DEBIT",
                LocalDateTime.now(),
                "Transfer to wallet " + toWalletId
        );
        transactions.get(fromWalletId).add(debitTx);

        // Perform credit to destination
        toWallet.setBalance(toWallet.getBalance() + amount);
        WalletTransactionResponseDTO creditTx = new WalletTransactionResponseDTO(
                request.transactionId() + "-CREDIT",
                amount,
                "CREDIT",
                LocalDateTime.now(),
                "Transfer from wallet " + fromWalletId
        );
        transactions.get(toWalletId).add(creditTx);

        log.info("Transfer completed successfully from wallet {} to wallet {}", fromWalletId, toWalletId);

        // Return debit transaction info for reference
        return debitTx;
    }

}
