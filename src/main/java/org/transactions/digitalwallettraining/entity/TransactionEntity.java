package org.transactions.digitalwallettraining.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class TransactionEntity {

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

    @Column(nullable = false)
    private String description;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate = LocalDateTime.now();

    @Column
    private String transactionId;


    public TransactionEntity() {}

    // Constructor for integration tests
    public TransactionEntity(WalletEntity wallet, TransactionType type, Double amount, String description) {
        this.wallet = wallet;
        this.type = type;
        setAmount(amount);
        this.description = description;
        this.transactionDate = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public WalletEntity getWallet() { return wallet; }


    public Double getAmount() {
        return amount;
    }

    // Setter with validation
    public void setAmount(Double amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Transaction amount must be greater than zero");
        }
        this.amount = amount;
    }


    public void setWallet(WalletEntity wallet) { this.wallet = wallet; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
}
