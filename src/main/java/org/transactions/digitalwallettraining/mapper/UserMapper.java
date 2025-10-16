package org.transactions.digitalwallettraining.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.transactions.digitalwallettraining.dto.UserRequestDTO;
import org.transactions.digitalwallettraining.dto.UserResponseDTO;
import org.transactions.digitalwallettraining.entity.UserEntity;

@Mapper
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "wallets", ignore = true)
    UserEntity toEntity(UserRequestDTO dto);

    UserResponseDTO toDTO(UserEntity entity);
}
