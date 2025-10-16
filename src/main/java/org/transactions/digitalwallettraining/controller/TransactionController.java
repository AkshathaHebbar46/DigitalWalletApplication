package org.transactions.digitalwallettraining.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.transactions.digitalwallettraining.dto.WalletTransactionRequestDTO;
import org.transactions.digitalwallettraining.dto.WalletTransactionResponseDTO;
import org.transactions.digitalwallettraining.service.WalletService;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    private final WalletService walletService;

    public TransactionController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/{walletId}/transactions")
    public ResponseEntity<WalletTransactionResponseDTO> processTransaction(
            @PathVariable Long walletId,
            @Valid @RequestBody WalletTransactionRequestDTO dto) {

        logger.info("Processing transaction for walletId={} with amount={}", walletId, dto.amount());

        // Delegate validation and processing to service
        WalletTransactionResponseDTO response = walletService.processTransaction(walletId, dto);

        logger.info("Transaction processed successfully for walletId={}, transactionId={}", walletId, response.transactionId());

        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{walletId}/transactions")
    public ResponseEntity<List<WalletTransactionResponseDTO>> listTransactions(@PathVariable Long walletId) {
        List<WalletTransactionResponseDTO> transactions = walletService.listTransactions(walletId);
        return ResponseEntity.ok(transactions);
    }
}
