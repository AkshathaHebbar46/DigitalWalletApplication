package org.transactions.digitalwallettraining.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.transactions.digitalwallettraining.model.WalletTransaction;
import org.transactions.digitalwallettraining.service.WalletService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final WalletService walletService;

    public TransactionController(WalletService walletService) {
        this.walletService = walletService;
    }

//    @GetMapping("/health")
//    public String health() {
//        return "Transactions are healthy";
//    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "activeTransactions", walletService.countActiveTransactions(),
                "timestamp", LocalDateTime.now()
        );
    }


    @PostMapping("/process")
    public ResponseEntity<String> processTransactions(@RequestBody List<WalletTransaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return ResponseEntity.badRequest().body("No transactions provided!");
        }
        walletService.process(transactions);
        return ResponseEntity.ok("Transactions processed successfully!");
    }

}
