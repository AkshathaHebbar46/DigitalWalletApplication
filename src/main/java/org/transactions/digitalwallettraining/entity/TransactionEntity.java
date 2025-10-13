package org.transactions.digitalwallettraining.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class TransactionEntity {

    public enum TransactionType { CREDIT, DEBIT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private WalletEntity wallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate = LocalDateTime.now();

    @Column(length = 255)
    private String description;

    // Constructors
    public TransactionEntity() {}
    public TransactionEntity(WalletEntity wallet, TransactionType type, Double amount, String description) {
        this.wallet = wallet;
        this.type = type;
        this.amount = amount;
        this.description = description;
    }

    // Getters and setters
    public Long getId() { return id; }
    public WalletEntity getWallet() { return wallet; }
    public void setWallet(WalletEntity wallet) { this.wallet = wallet; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
