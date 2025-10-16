package org.transactions.digitalwallettraining.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.transactions.digitalwallettraining.validation.ValidTransactionAmount;

public record WalletTransactionRequestDTO(
        String transactionId,

        @NotNull(message = "Transaction amount cannot be null")
        @ValidTransactionAmount
        Double amount,

        @NotBlank(message = "Transaction type is required")
        String type,

        @NotBlank(message = "Description is required")
        String description
) {}
