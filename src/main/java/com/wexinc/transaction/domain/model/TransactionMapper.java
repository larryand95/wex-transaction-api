package com.wexinc.transaction.domain.model;

import com.wexinc.transaction.domain.entity.PurchaseTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TransactionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "purchaseAmount", expression = "java(roundToTwoDecimalPlaces(request.getPurchaseAmount()))")
    PurchaseTransaction toEntity(CreateTransactionRequest request);

    TransactionResponse toResponse(PurchaseTransaction entity);

    default BigDecimal roundToTwoDecimalPlaces(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
