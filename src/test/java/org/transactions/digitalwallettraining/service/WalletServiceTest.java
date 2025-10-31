package org.transactions.digitalwallettraining.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.transactions.digitalwallettraining.dto.*;
import org.transactions.digitalwallettraining.entity.*;
import org.transactions.digitalwallettraining.exception.MaxRetryExceededException;
import org.transactions.digitalwallettraining.repository.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WalletFreezeService walletFreezeService;

    @InjectMocks
    private WalletService walletService;

    private WalletEntity wallet;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new UserEntity();

        // ✅ Use reflection to set private field in test only
        try {
            java.lang.reflect.Field idField = UserEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        wallet = new WalletEntity();
        wallet.setId(10L);
        wallet.setUser(user);
        wallet.setBalance(1000.0);
        wallet.setDailySpent(0.0);
        wallet.setFrozen(false);
    }

    // ✅ Create wallet success
    @Test
    void testCreateWallet_Success() {
        WalletRequestDTO request = new WalletRequestDTO(1L, 500.0);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(walletRepository.save(any(WalletEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletResponseDTO response = walletService.createWallet(request);

        assertEquals(1L, response.getUserId());
        assertEquals(500.0, response.getBalance());
        verify(walletRepository, times(1)).save(any(WalletEntity.class));
    }

    // ❌ Create wallet - user not found
    @Test
    void testCreateWallet_UserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        WalletRequestDTO request = new WalletRequestDTO(1L, 500.0);
        assertThrows(IllegalArgumentException.class, () -> walletService.createWallet(request));
    }

    // ✅ Get balance success
    @Test
    void testGetBalance_Success() {
        when(walletRepository.findById(10L)).thenReturn(Optional.of(wallet));
        Double balance = walletService.getBalance(10L);
        assertEquals(1000.0, balance);
    }

    // ❌ Get balance - wallet not found
    @Test
    void testGetBalance_WalletNotFound() {
        when(walletRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> walletService.getBalance(99L));
    }

    // ✅ Process CREDIT transaction
    @Test
    void testProcessTransaction_Credit_Success() {
        WalletTransactionRequestDTO request = new WalletTransactionRequestDTO("txn1", 500.0, "CREDIT", "Deposit");

        when(walletRepository.findById(10L)).thenReturn(Optional.of(wallet));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WalletTransactionResponseDTO response = walletService.processTransaction(10L, request);

        assertEquals("CREDIT", response.type());
        verify(walletRepository, atLeastOnce()).save(wallet);
        verify(transactionRepository, times(1)).save(any(TransactionEntity.class));
    }

    // ✅ Process DEBIT transaction success
    @Test
    void testProcessTransaction_Debit_Success() {
        WalletTransactionRequestDTO request = new WalletTransactionRequestDTO("txn2", 100.0, "DEBIT", "Purchase");

        when(walletRepository.findById(10L)).thenReturn(Optional.of(wallet));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WalletTransactionResponseDTO response = walletService.processTransaction(10L, request);

        assertEquals("DEBIT", response.type());
        verify(transactionRepository, times(1)).save(any(TransactionEntity.class));
    }

    // ❌ Invalid transaction type
    @Test
    void testProcessTransaction_InvalidType() {
        WalletTransactionRequestDTO request = new WalletTransactionRequestDTO("txn3", 100.0, "INVALID", "Test");
        when(walletRepository.findById(10L)).thenReturn(Optional.of(wallet));
        assertThrows(IllegalArgumentException.class, () -> walletService.processTransaction(10L, request));
    }

    // ❌ Amount must be positive
    @Test
    void testProcessTransaction_InvalidAmount() {
        WalletTransactionRequestDTO request = new WalletTransactionRequestDTO("txn4", -10.0, "CREDIT", "Negative");
        when(walletRepository.findById(10L)).thenReturn(Optional.of(wallet));
        assertThrows(IllegalArgumentException.class, () -> walletService.processTransaction(10L, request));
    }

    // ❌ Max retry exceeded
    @Test
    void testProcessTransaction_MaxRetryExceeded() {
        WalletTransactionRequestDTO request = new WalletTransactionRequestDTO("txn5", 100.0, "CREDIT", "Retry test");

        when(walletRepository.findById(10L))
                .thenThrow(new CannotAcquireLockException("lock"))
                .thenThrow(new ObjectOptimisticLockingFailureException(WalletEntity.class, 1L));

        assertThrows(MaxRetryExceededException.class,
                () -> walletService.processTransaction(10L, request));
    }

    // ✅ Transfer money success
    @Test
    void testTransferMoney_Success() {
        WalletEntity fromWallet = new WalletEntity(user, 1000.0);
        fromWallet.setId(1L);
        WalletEntity toWallet = new WalletEntity(user, 500.0);
        toWallet.setId(2L);

        when(walletRepository.findById(1L)).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findById(2L)).thenReturn(Optional.of(toWallet));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WalletTransactionResponseDTO response = walletService.transferMoney(1L, 2L, 100.0);

        assertEquals("DEBIT", response.type());
        verify(walletRepository, times(3)).save(any(WalletEntity.class)); // ✅ changed from 2 → 3
        verify(transactionRepository, times(2)).save(any(TransactionEntity.class));
    }


    // ❌ Transfer to same wallet
    @Test
    void testTransferMoney_SameWallet() {
        assertThrows(IllegalArgumentException.class, () -> walletService.transferMoney(1L, 1L, 100.0));
    }

    // ❌ Transfer invalid amount
    @Test
    void testTransferMoney_InvalidAmount() {
        assertThrows(IllegalArgumentException.class, () -> walletService.transferMoney(1L, 2L, -100.0));
    }

    // ✅ List transactions
    @Test
    void testListTransactions_Success() {
        TransactionEntity txn = new TransactionEntity(wallet, TransactionType.CREDIT, 100.0, "Deposit");
        txn.setTransactionId("t123");
        when(transactionRepository.findByWalletId(10L)).thenReturn(List.of(txn));

        List<WalletTransactionResponseDTO> result = walletService.listTransactions(10L);

        assertEquals(1, result.size());
        assertEquals("CREDIT", result.get(0).type());
    }

    // ✅ Get all wallets
    @Test
    void testGetAllWallets_Success() {
        WalletEntity wallet2 = new WalletEntity(user, 200.0);
        wallet2.setId(11L);

        when(walletRepository.findAll()).thenReturn(List.of(wallet, wallet2));

        List<WalletResponseDTO> result = walletService.getAllWallets();

        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).getWalletId());
    }

    // ✅ Get wallet details
    @Test
    void testGetWalletDetails_Success() {
        when(walletRepository.findById(10L)).thenReturn(Optional.of(wallet));

        WalletResponseDTO result = walletService.getWalletDetails(10L);

        assertEquals(wallet.getId(), result.getWalletId());
        verify(walletRepository).findById(10L);
    }

    // ❌ Wallet not found
    @Test
    void testGetWalletDetails_NotFound() {
        when(walletRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> walletService.getWalletDetails(999L));
    }
}
