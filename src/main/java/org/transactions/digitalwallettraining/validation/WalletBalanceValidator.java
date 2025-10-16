package org.transactions.digitalwallettraining.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.transactions.digitalwallettraining.dto.WalletTransactionRequestDTO;

public class WalletBalanceValidator implements ConstraintValidator<ValidWalletBalance, WalletTransactionRequestDTO> {

    @Override
    public boolean isValid(WalletTransactionRequestDTO dto, ConstraintValidatorContext context) {
        // Skip if amount is null, @NotNull will handle
        if (dto.amount() == null) return true;

        // NOTE: cannot check wallet balance here without walletId
        // Must validate balance in service layer
        return true;
    }
}
