package org.transactions.digitalwallettraining.service;

import org.springframework.stereotype.Service;
import org.transactions.digitalwallettraining.dto.*;

import java.util.*;

@Service
public class WalletService {

    private final Map<Long, WalletResponseDTO> wallets = new HashMap<>();
    private final Map<Long, List<WalletTransactionResponseDTO>> transactions = new HashMap<>();
    private Long walletCounter = 1L;

    // Create a wallet
    public WalletResponseDTO createWallet(WalletRequestDTO request) {
        Long walletId = walletCounter++;
        WalletResponseDTO wallet = new WalletResponseDTO(walletId, request.getUserId(), request.getBalance());
        wallets.put(walletId, wallet);
        transactions.put(walletId, new ArrayList<>());
        return wallet;
    }

    // Get wallet balance
    public Double getBalance(Long walletId) {
        WalletResponseDTO wallet = wallets.get(walletId);
        if (wallet == null) throw new NoSuchElementException("Wallet not found");
        return wallet.getBalance();
    }

    // Process a transaction
    public WalletTransactionResponseDTO processTransaction(Long walletId, WalletTransactionRequestDTO request) {
        WalletResponseDTO wallet = wallets.get(walletId);
        if (wallet == null) throw new NoSuchElementException("Wallet not found");

        Double amount = request.amount();
        String type = request.type().toUpperCase();

        if ("CREDIT".equals(type)) {
            wallet.setBalance(wallet.getBalance() + amount);
        } else if ("DEBIT".equals(type)) {
            if (wallet.getBalance() < amount) {
                throw new IllegalArgumentException("Insufficient wallet balance");
            }
            wallet.setBalance(wallet.getBalance() - amount);
        } else {
            throw new IllegalArgumentException("Transaction type must be CREDIT or DEBIT");
        }

        WalletTransactionResponseDTO response = new WalletTransactionResponseDTO(
                request.transactionId(),
                request.amount(),
                request.type(),
                null,
                request.description()
        );

        transactions.get(walletId).add(response);
        return response;
    }

    // List transactions
    public List<WalletTransactionResponseDTO> listTransactions(Long walletId) {
        if (!transactions.containsKey(walletId)) throw new NoSuchElementException("Wallet not found");
        return transactions.get(walletId);
    }
}
