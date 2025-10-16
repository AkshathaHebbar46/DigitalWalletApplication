package org.transactions.digitalwallettraining.dto;

public class WalletResponseDTO {

    private Long walletId;
    private Long userId;
    private Double balance;

    public WalletResponseDTO(Long walletId, Long userId, Double balance) {
        this.walletId = walletId;
        this.userId = userId;
        this.balance = balance;
    }

    public Long getWalletId() { return walletId; }
    public void setWalletId(Long walletId) { this.walletId = walletId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }
}
