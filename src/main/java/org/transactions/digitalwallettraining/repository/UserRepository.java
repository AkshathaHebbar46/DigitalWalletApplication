package org.transactions.digitalwallettraining.repository;

import org.springframework.data.jpa.repository.Query;
import org.transactions.digitalwallettraining.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    // Custom query method to find a user by email
    Optional<UserEntity> findByEmail(String email);

    // Find users by name containing a substring (case-insensitive)
    List<UserEntity> findByNameContainingIgnoreCase(String name);

    // Find users created after a certain date
    List<UserEntity> findByCreatedAtAfter(java.time.LocalDateTime date);

    // Find users who have at least one wallet with balance > 5000
    @Query("SELECT u FROM UserEntity u JOIN u.wallets w WHERE w.balance > 5000")
    List<UserEntity> findUsersWithHighBalanceWallets();
}
