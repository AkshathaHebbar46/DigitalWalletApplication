package org.transactions.digitalwallettraining.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
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

    @Version
    @Column(nullable = false)
    private Long version = 0L; // for optimistic locking

    // ✅ Daily tracking fields
    @Column(name = "daily_spent", nullable = false)
    private Double dailySpent = 0.0;

    @Column(name = "last_transaction_date")
    private LocalDate lastTransactionDate = LocalDate.now();

    // ✅ Freeze logic
    @Column(name = "frozen", nullable = false)
    private Boolean frozen = false;

    @Column(name = "frozen_at")
    private LocalDateTime frozenAt;

    // Wallet creation date
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransactionEntity> transactions = new ArrayList<>();

    // --- Constructors ---
    public WalletEntity() {}

    public WalletEntity(UserEntity user) {
        this.user = user;
        this.balance = 0.0;
        this.dailySpent = 0.0;
        this.frozen = false;
        this.lastTransactionDate = LocalDate.now();
    }

    public WalletEntity(UserEntity user, Double balance) {
        this.user = user;
        this.balance = balance != null && balance >= 0 ? balance : 0.0;
        this.dailySpent = 0.0;
        this.frozen = false;
        this.lastTransactionDate = LocalDate.now();
    }

    // --- Helper Methods ---
    public void addTransaction(TransactionEntity t) {
        transactions.add(t);
        t.setWallet(this);
    }

    public void removeTransaction(TransactionEntity t) {
        transactions.remove(t);
        t.setWallet(null);
    }

    /**
     * ✅ Reset daily spent and frozen status if it's a new day.
     */
    public void resetDailyIfNewDay() {
        LocalDate today = LocalDate.now();
        if (lastTransactionDate == null || !lastTransactionDate.equals(today)) {
            this.dailySpent = 0.0;
            this.frozen = false;
            this.frozenAt = null;
            this.lastTransactionDate = today;
        }
    }

    /**
     * ✅ Automatically unfreeze and reset limit after 2 minutes.
     */
    public void checkAndUnfreeze() {
        if (Boolean.TRUE.equals(this.frozen) && this.frozenAt != null) {
            LocalDateTime now = LocalDateTime.now();
            if (this.frozenAt.plusMinutes(2).isBefore(now)) {
                this.frozen = false;
                this.frozenAt = null;
                this.dailySpent = 0.0; // 💥 reset spent amount
            }
        }
    }


    // --- Getters & Setters ---
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Double getBalance() { return balance; }

    public void setBalance(Double balance) {
        if (balance < 0) throw new IllegalArgumentException("Balance cannot be negative");
        this.balance = balance;
    }

    public Long getVersion() { return version; }

    public void setVersion(Long version) { this.version = version; }

    public Double getDailySpent() { return dailySpent; }

    public void setDailySpent(Double dailySpent) { this.dailySpent = dailySpent; }

    public LocalDate getLastTransactionDate() { return lastTransactionDate; }

    public void setLastTransactionDate(LocalDate lastTransactionDate) { this.lastTransactionDate = lastTransactionDate; }

    public Boolean getFrozen() { return frozen; }

    public void setFrozen(Boolean frozen) { this.frozen = frozen; }

    public LocalDateTime getFrozenAt() { return frozenAt; }

    public void setFrozenAt(LocalDateTime frozenAt) { this.frozenAt = frozenAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public UserEntity getUser() { return user; }

    public void setUser(UserEntity user) { this.user = user; }

    public List<TransactionEntity> getTransactions() { return transactions; }

    public void setTransactions(List<TransactionEntity> transactions) { this.transactions = transactions; }
}
