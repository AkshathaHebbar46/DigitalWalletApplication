package org.transactions.digitalwallettraining.dto;

public record WalletTransferRequestDTO(
        Long fromWalletId,
        Long toWalletId,
        Double amount,
        String transactionId,
        String description
) {}
