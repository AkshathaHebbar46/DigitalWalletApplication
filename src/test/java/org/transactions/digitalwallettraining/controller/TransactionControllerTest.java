package org.transactions.digitalwallettraining.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.transactions.digitalwallettraining.service.WalletService;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WalletService walletService;

    // HEALTH CHECK – Normal case
    @Test
    void testHealthEndpointReturnsValidResponse() throws Exception {
        when(walletService.countActiveTransactions()).thenReturn(5);

        mockMvc.perform(get("/transactions/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.activeTransactions").value(5))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // HEALTH CHECK – Boundary case (0 transactions)
    @Test
    void testHealthEndpointWhenNoTransactions() throws Exception {
        when(walletService.countActiveTransactions()).thenReturn(0);

        mockMvc.perform(get("/transactions/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeTransactions").value(0));
    }

    // PROCESS – Valid single transaction
    @Test
    void testProcessSingleTransactionSuccess() throws Exception {
        String request = """
                [
                    {"transactionId": "TXN001", "amount": 100.0, "type": "CREDIT", "timestamp": "2025-10-07T10:00:00"}
                ]
                """;

        mockMvc.perform(post("/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(content().string("Transactions processed successfully!"));

        verify(walletService, times(1)).process(anyList());
    }

    // PROCESS – Multiple transactions (normal case)
    @Test
    void testProcessMultipleTransactions() throws Exception {
        String request = """
                [
                    {"transactionId": "TXN001", "amount": 50.0, "type": "DEBIT", "timestamp": "2025-10-07T10:00:00"},
                    {"transactionId": "TXN002", "amount": 100.0, "type": "CREDIT", "timestamp": "2025-10-07T11:00:00"}
                ]
                """;

        mockMvc.perform(post("/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(content().string("Transactions processed successfully!"));

        verify(walletService, times(1)).process(anyList());
    }

    // PROCESS – Empty list (boundary case)
    @Test
    void testProcessEmptyTransactionList() throws Exception {
        mockMvc.perform(post("/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No transactions provided!"));

        verify(walletService, never()).process(anyList());
    }

    @Test
    void testProcessNullRequestBody() throws Exception {
        mockMvc.perform(post("/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null")) // explicitly null
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Provide valid transaction details!"));

        verify(walletService, never()).process(anyList());
    }

    // PROCESS – Invalid JSON structure (error case)
    @Test
    void testProcessMalformedJson() throws Exception {
        String invalidJson = """
                [
                    {"transactionId": "TXN001", "amount": 100.0, "type": "CREDIT"
                """; // missing closing bracket and quotes

        mockMvc.perform(post("/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    // PROCESS – Zero amount transaction (boundary case)
    @Test
    void testProcessZeroAmountTransaction() throws Exception {
        String request = """
                [
                    {"transactionId": "TXN003", "amount": 0, "type": "DEBIT", "timestamp": "2025-10-07T10:00:00"}
                ]
                """;

        mockMvc.perform(post("/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Provide valid transaction details!"));

        verify(walletService, never()).process(anyList());
    }

    @Test
    void testProcessNegativeAmountTransaction() throws Exception {
        String request = """
            [
                {"transactionId": "TXN004", "amount": -50, "type": "CREDIT", "timestamp": "2025-10-07T10:00:00"}
            ]
            """;

        mockMvc.perform(post("/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Provide valid transaction details!"));

        verify(walletService, never()).process(anyList());
    }


    // PROCESS – Large number of transactions (stress test)
    @Test
    void testProcessLargeTransactionList() throws Exception {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < 1000; i++) {
            json.append(String.format("{\"transactionId\":\"TXN%d\",\"amount\":10.0,\"type\":\"CREDIT\",\"timestamp\":\"2025-10-07T10:00:00\"},", i));
        }
        json.deleteCharAt(json.length() - 1); // remove last comma
        json.append("]");

        mockMvc.perform(post("/transactions/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string("Transactions processed successfully!"));

        verify(walletService, times(1)).process(anyList());
    }
}
