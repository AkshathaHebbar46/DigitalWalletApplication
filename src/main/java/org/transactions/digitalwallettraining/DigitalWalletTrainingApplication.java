package org.transactions.digitalwallettraining;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.transactions.digitalwallettraining.model.WalletTransaction;
import org.transactions.digitalwallettraining.service.TransactionProcessor;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootApplication
public class DigitalWalletTrainingApplication {

    public static void main(String[] args) {
        SpringApplication.run(DigitalWalletTrainingApplication.class, args);
    }

    @Bean
    CommandLineRunner runTransactions() {
        return args -> {
            List<WalletTransaction> transactions = List.of(
                    new WalletTransaction("TXN001", 100, "CREDIT", LocalDateTime.now()),
                    new WalletTransaction("TXN002", 50, "DEBIT", LocalDateTime.now()),
                    new WalletTransaction("TXN003", 200, "CREDIT", LocalDateTime.now())
            );

            // Use the TransactionProcessor service
            TransactionProcessor processor = new TransactionProcessor();
            processor.processTransactions(transactions);
        };
    }
}
