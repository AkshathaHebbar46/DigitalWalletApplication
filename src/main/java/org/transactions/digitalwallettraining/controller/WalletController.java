package org.transactions.digitalwallettraining.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.transactions.digitalwallettraining.dto.*;
import org.transactions.digitalwallettraining.service.WalletService;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    // Create wallet
    @PostMapping
    public ResponseEntity<WalletResponseDTO> createWallet(@RequestBody WalletRequestDTO request) {
        WalletResponseDTO wallet = walletService.createWallet(request);
        return ResponseEntity.status(201).body(wallet);
    }

    // Get balance
    @GetMapping("/{walletId}/balance")
    public ResponseEntity<Double> getBalance(@PathVariable Long walletId) {
        Double balance = walletService.getBalance(walletId);
        return ResponseEntity.ok(balance);
    }

    // Process transaction
    @PostMapping("/{walletId}/transactions")
    public ResponseEntity<WalletTransactionResponseDTO> processTransaction(
            @PathVariable Long walletId,
            @Valid @RequestBody WalletTransactionRequestDTO request) {

        WalletTransactionResponseDTO txn = walletService.processTransaction(walletId, request);
        return ResponseEntity.status(201).body(txn);
    }

    // List transactions
    @GetMapping("/{walletId}/transactions")
    public ResponseEntity<List<WalletTransactionResponseDTO>> listTransactions(@PathVariable Long walletId) {
        List<WalletTransactionResponseDTO> list = walletService.listTransactions(walletId);
        return ResponseEntity.ok(list);
    }

    public void transferMoney(Long fromWalletId, Long toWalletId, Double amount) {
        if (fromWalletId.equals(toWalletId)) {
            throw new IllegalArgumentException("Cannot transfer to the same wallet");
        }

        // Create debit transaction
        WalletTransactionRequestDTO debitRequest = new WalletTransactionRequestDTO(
                "TXN-DEBIT-" + System.currentTimeMillis(),
                amount,
                "DEBIT",
                "Transfer to wallet " + toWalletId
        );
        processTransaction(fromWalletId, debitRequest);

        // Create credit transaction
        WalletTransactionRequestDTO creditRequest = new WalletTransactionRequestDTO(
                "TXN-CREDIT-" + System.currentTimeMillis(),
                amount,
                "CREDIT",
                "Transfer from wallet " + fromWalletId
        );
        processTransaction(toWalletId, creditRequest);
    }
    // Transfer money between wallets
    @PostMapping("/transfer")
    public ResponseEntity<WalletTransactionResponseDTO> transferMoney(
            @RequestBody @Valid WalletTransferRequestDTO request) {

        WalletTransactionResponseDTO debitTx = walletService.transferMoney(
                request.fromWalletId(),
                request.toWalletId(),
                new WalletTransactionRequestDTO(
                        null,
                        request.amount(),
                        "DEBIT",
                        "Transfer request"
                )
        );
        return ResponseEntity.status(201).body(debitTx);
    }




}
