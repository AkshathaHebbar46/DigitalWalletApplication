package org.transactions.digitalwallettraining.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.transactions.digitalwallettraining.dto.WalletTransactionRequestDTO;
import org.transactions.digitalwallettraining.dto.WalletTransactionResponseDTO;
import org.transactions.digitalwallettraining.entity.TransactionType;
import org.transactions.digitalwallettraining.service.TransactionService;
import org.transactions.digitalwallettraining.service.WalletService;

import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // ✅ Paginated & filtered transaction history
    @GetMapping("/history")
    public ResponseEntity<Page<WalletTransactionResponseDTO>> getTransactionHistory(
            @RequestParam Long walletId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<WalletTransactionResponseDTO> transactions =
                transactionService.getFilteredTransactions(walletId, type, startDate, endDate, pageable);

        return ResponseEntity.ok(transactions);
    }
}
