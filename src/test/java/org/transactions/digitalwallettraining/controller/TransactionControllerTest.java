package org.transactions.digitalwallettraining.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.transactions.digitalwallettraining.dto.WalletTransactionRequestDTO;
import org.transactions.digitalwallettraining.dto.WalletTransactionResponseDTO;
import org.transactions.digitalwallettraining.service.WalletService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WalletService walletService;

    @Test
    void testHealthEndpointReturnsValidResponse() throws Exception {
        WalletTransactionResponseDTO txn1 = new WalletTransactionResponseDTO("TXN001", 100.0, "CREDIT", LocalDateTime.now(), "Salary");
        WalletTransactionResponseDTO txn2 = new WalletTransactionResponseDTO("TXN002", 50.0, "DEBIT", LocalDateTime.now(), "Groceries");

        when(walletService.listTransactions(1L)).thenReturn(List.of(txn1, txn2));

        mockMvc.perform(get("/wallets/1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.activeTransactions").value(2))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testProcessSingleTransactionSuccess() throws Exception {
        WalletTransactionRequestDTO requestDTO = new WalletTransactionRequestDTO(
                "TXN001", 100.0, "CREDIT",  "Salary"
        );

        WalletTransactionResponseDTO responseDTO = new WalletTransactionResponseDTO(
                "TXN001", 100.0, "CREDIT", LocalDateTime.now(), "Salary"
        );

        when(walletService.processTransaction(eq(1L), any(WalletTransactionRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/wallets/1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("TXN001"))
                .andExpect(jsonPath("$.amount").value(100.0))
                .andExpect(jsonPath("$.type").value("CREDIT"));

        verify(walletService, times(1)).processTransaction(eq(1L), any(WalletTransactionRequestDTO.class));
    }

    @Test
    void testListTransactions() throws Exception {
        WalletTransactionResponseDTO txn1 = new WalletTransactionResponseDTO("TXN001", 100.0, "CREDIT", LocalDateTime.now(), "Salary");
        WalletTransactionResponseDTO txn2 = new WalletTransactionResponseDTO("TXN002", 50.0, "DEBIT", LocalDateTime.now(), "Groceries");

        when(walletService.listTransactions(1L)).thenReturn(List.of(txn1, txn2));

        mockMvc.perform(get("/wallets/1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].transactionId").value("TXN001"))
                .andExpect(jsonPath("$[1].transactionId").value("TXN002"));

        verify(walletService, times(1)).listTransactions(1L);
    }
}
