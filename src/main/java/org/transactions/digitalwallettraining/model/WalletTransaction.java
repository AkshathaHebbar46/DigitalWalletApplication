package org.transactions.digitalwallettraining.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WalletTransaction (
        String transactionId,
        double amount,
        String type,
        LocalDateTime timestamp

){
    public WalletTransaction {
        if(transactionId == null || transactionId.isBlank()){
            throw new IllegalArgumentException("transactionId cannot be null");
        }
        if(amount <= 0){
            throw new IllegalArgumentException("amount cannot be negative");
        }
        if(!type.equals("CREDIT") &&  !type.equals("DEBIT")){
            throw new IllegalArgumentException("type must be either CREDIT or DEBIT");
        }
        if(timestamp == null){
            throw new IllegalArgumentException("timestamp cannot be null");
        }
    }
}

