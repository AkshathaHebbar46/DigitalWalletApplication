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
}
