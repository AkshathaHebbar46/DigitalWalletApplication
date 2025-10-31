package org.transactions.digitalwallettraining.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.transactions.digitalwallettraining.entity.WalletEntity;
import org.transactions.digitalwallettraining.repository.WalletRepository;

import java.time.LocalDateTime;

@Service
public class WalletFreezeService {

    private static final Logger log = LoggerFactory.getLogger(WalletFreezeService.class);
    private final WalletRepository walletRepository;

    public WalletFreezeService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    // 🔒 Freeze wallet immediately in a new transaction
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = Exception.class)
    public void freezeWallet(WalletEntity wallet) {
        try {
            WalletEntity fresh = walletRepository.findById(wallet.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

            fresh.setFrozen(true);
            fresh.setFrozenAt(LocalDateTime.now());
            walletRepository.saveAndFlush(fresh);

            log.warn("🚨 Wallet {} frozen at {}", fresh.getId(), fresh.getFrozenAt());
        }
        catch (OptimisticLockingFailureException e) {
            log.warn("⚠️ Optimistic lock detected while freezing wallet {}. Retrying once...", wallet.getId());

            try {
                // Retry once safely
                WalletEntity retry = walletRepository.findById(wallet.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));
                retry.setFrozen(true);
                retry.setFrozenAt(LocalDateTime.now());
                walletRepository.saveAndFlush(retry);
                log.info("✅ Wallet {} frozen successfully after retry.", wallet.getId());
            } catch (Exception ex) {
                log.error("❌ Could not freeze wallet {} even after retry: {}", wallet.getId(), ex.getMessage());
            }
        }
        catch (Exception ex) {
            log.error("❌ Unexpected error freezing wallet {}: {}", wallet.getId(), ex.getMessage());
        }
    }

    // 🧊 Unfreeze wallet in a new transaction
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = Exception.class)
    public void unfreezeWallet(WalletEntity wallet) {
        try {
            WalletEntity fresh = walletRepository.findById(wallet.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

            fresh.setFrozen(false);
            fresh.setFrozenAt(null);
            fresh.setDailySpent(0.0);
            walletRepository.saveAndFlush(fresh);

            log.info("🧊 Wallet {} unfrozen successfully.", wallet.getId());
        }
        catch (OptimisticLockingFailureException e) {
            log.warn("⚠️ Optimistic lock conflict while unfreezing wallet {} — ignoring.", wallet.getId());
        }
        catch (Exception e) {
            log.error("❌ Unexpected error unfreezing wallet {}: {}", wallet.getId(), e.getMessage());
        }
    }
}
