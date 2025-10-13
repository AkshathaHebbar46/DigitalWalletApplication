package org.transactions.digitalwallettraining.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wallets")
public class WalletEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double balance = 0.0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // --- Relationship: Wallet belongs to a user ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // --- Relationship: Wallet has many transactions ---
    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TransactionEntity> transactions = new ArrayList<>();

    // Constructors
    public WalletEntity() {}
    public WalletEntity(UserEntity user, Double balance) {
        this.user = user;
        this.balance = balance;
    }

    // Helper methods
    public void addTransaction(TransactionEntity transaction) {
        transactions.add(transaction);
        transaction.setWallet(this);
    }

    public void removeTransaction(TransactionEntity transaction) {
        transactions.remove(transaction);
        transaction.setWallet(null);
    }

    // Getters and setters
    public Long getId() { return id; }
    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }
    public List<TransactionEntity> getTransactions() { return transactions; }
}
