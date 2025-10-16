package org.transactions.digitalwallettraining.dto;

import java.time.LocalDateTime;

public record WalletTransactionResponseDTO(
        String transactionId,
        Double amount,
        String type,
        LocalDateTime timestamp,
        String description
) {
    // Compact constructor to auto-set timestamp if null
    public WalletTransactionResponseDTO {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
