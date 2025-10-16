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

    @Column(nullable = false)
    private Integer age;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WalletEntity> wallets = new ArrayList<>();

    // Constructors
    public UserEntity() {}

    public UserEntity(String name, String email, Integer age) {
        this.name = name;
        this.email = email;
        this.age = age;
    }

    // Relationship helpers
    public void addWallet(WalletEntity wallet) {
        wallets.add(wallet);
        wallet.setUser(this); // maintain bidirectional relationship
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
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public List<WalletEntity> getWallets() { return wallets; }
    public void setWallets(List<WalletEntity> wallets) { this.wallets = wallets; }
}
