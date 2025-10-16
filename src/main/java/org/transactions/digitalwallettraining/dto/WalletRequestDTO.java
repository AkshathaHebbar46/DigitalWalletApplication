package org.transactions.digitalwallettraining.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class WalletRequestDTO {

    @NotNull(message = "UserId is required")
    private Long userId;

    @Min(value = 0, message = "Initial balance cannot be negative")
    private Double balance;

    public WalletRequestDTO() {}

    public WalletRequestDTO(Long userId, Double balance) {
        this.userId = userId;
        this.balance = balance;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }
}
