package org.transactions.digitalwallettraining.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.transactions.digitalwallettraining.dto.WalletTransactionRequestDTO;
import org.transactions.digitalwallettraining.dto.WalletTransactionResponseDTO;
import org.transactions.digitalwallettraining.entity.TransactionEntity;

@Mapper(componentModel = "spring")
public interface WalletTransactionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "wallet", ignore = true)
    @Mapping(target = "transactionDate", expression = "java(java.time.LocalDateTime.now())")
    TransactionEntity toEntity(WalletTransactionRequestDTO dto);

    @Mapping(source = "transactionDate", target = "timestamp") // map entity field → DTO field
    WalletTransactionResponseDTO toDTO(TransactionEntity entity);
}
