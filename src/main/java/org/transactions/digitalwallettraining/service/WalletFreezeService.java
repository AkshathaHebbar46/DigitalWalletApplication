package org.transactions.digitalwallettraining.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = Exception.class)
    public void freezeWallet(WalletEntity wallet) {
        wallet.setFrozen(true);
        wallet.setFrozenAt(LocalDateTime.now());
        walletRepository.saveAndFlush(wallet);
        log.warn("🚨 Wallet {} frozen at {}", wallet.getId(), wallet.getFrozenAt());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void unfreezeWallet(WalletEntity wallet) {
        wallet.setFrozen(false);
        wallet.setFrozenAt(null);
        wallet.setDailySpent(0.0);
        walletRepository.saveAndFlush(wallet);
        log.info("🧊 Wallet {} unfrozen successfully.", wallet.getId());
    }
}
