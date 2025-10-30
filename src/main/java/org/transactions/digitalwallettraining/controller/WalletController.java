package org.transactions.digitalwallettraining.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.transactions.digitalwallettraining.dto.*;
import org.transactions.digitalwallettraining.service.WalletService;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/wallets")
public class WalletController {

    private static final Logger log = LoggerFactory.getLogger(WalletController.class);
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    // 🔹 Create wallet manually (if needed)
    @PostMapping
    public ResponseEntity<WalletResponseDTO> createWallet(@RequestBody @Valid WalletRequestDTO request) {
        log.info("Received request to create wallet for userId={}", request.getUserId());
        WalletResponseDTO wallet = walletService.createWallet(request);
        log.info("Wallet created successfully for userId={}, walletId={}", request.getUserId(), wallet.getWalletId());
        return ResponseEntity.status(201).body(wallet);
    }

    // 🔹 Get wallet balance
    @GetMapping("/{walletId}/balance")
    public ResponseEntity<Double> getBalance(@PathVariable Long walletId) {
        log.info("Fetching wallet balance for walletId={}", walletId);
        Double balance = walletService.getBalance(walletId);
        return ResponseEntity.ok(balance);
    }

    // 🔹 Process CREDIT / DEBIT
    @PostMapping("/{walletId}/transactions")
    public ResponseEntity<WalletTransactionResponseDTO> processTransaction(
            @PathVariable Long walletId,
            @Valid @RequestBody WalletTransactionRequestDTO request) {

        log.info("Processing {} transaction for walletId={} with amount={}",
                request.type(), walletId, request.amount());
        WalletTransactionResponseDTO txn = walletService.processTransaction(walletId, request);
        log.info("{} transaction completed for walletId={}, txnId={}", request.type(), walletId, txn.transactionId());
        return ResponseEntity.status(201).body(txn);
    }

    // 🔹 List all transactions for a wallet
    @GetMapping("/{walletId}/list-transactions")
    public ResponseEntity<List<WalletTransactionResponseDTO>> listTransactions(@PathVariable Long walletId) {
        log.info("Listing all transactions for walletId={}", walletId);
        List<WalletTransactionResponseDTO> list = walletService.listTransactions(walletId);
        return ResponseEntity.ok(list);
    }

    // 🔹 Transfer money between wallets
    @PostMapping("/transfer")
    public ResponseEntity<WalletTransactionResponseDTO> transferMoney(
            @RequestBody @Valid WalletTransferRequestDTO request) {
        log.info("Received transfer request: {} → {} | amount={}",
                request.fromWalletId(), request.toWalletId(), request.amount());

        WalletTransactionResponseDTO response = walletService.transferMoney(
                request.fromWalletId(),
                request.toWalletId(),
                request.amount()
        );

        log.info("Transfer processed successfully between {} and {}",
                request.fromWalletId(), request.toWalletId());

        return ResponseEntity.status(201).body(response);
    }


    // 🔹 Get wallet details (includes user info, balance, and status)
    @GetMapping("/{walletId}")
    public ResponseEntity<WalletResponseDTO> getWalletDetails(@PathVariable Long walletId) {
        log.info("Fetching wallet details for walletId={}", walletId);
        WalletResponseDTO walletDetails = walletService.getWalletDetails(walletId);
        return ResponseEntity.ok(walletDetails);
    }
    // 🔹 Get All Wallets
    @GetMapping
    public ResponseEntity<List<WalletResponseDTO>> getAllWallets() {
        log.info("Fetching all wallets");
        List<WalletResponseDTO> wallets = walletService.getAllWallets();
        log.info("Retrieved {} wallets", wallets.size());
        return ResponseEntity.ok(wallets);
    }

}
