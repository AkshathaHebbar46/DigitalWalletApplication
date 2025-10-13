package org.transactions.digitalwallettraining.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // --- Relationship: User has many wallets ---
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<WalletEntity> wallets = new ArrayList<>();

    // Constructors
    public UserEntity() {}
    public UserEntity(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // --- Helper method to add wallet ---
    public void addWallet(WalletEntity wallet) {
        wallets.add(wallet);
        wallet.setUser(this);
    }

    public void removeWallet(WalletEntity wallet) {
        wallets.remove(wallet);
        wallet.setUser(null);
    }

    // Getters and setters
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<WalletEntity> getWallets() { return wallets; }
}
