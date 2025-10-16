package org.transactions.digitalwallettraining.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TransactionAmountValidator implements ConstraintValidator<ValidTransactionAmount, Double> {

    @Override
    public boolean isValid(Double amount, ConstraintValidatorContext context) {
        if (amount == null) return true; // let @NotNull handle it
        return amount > 0;
    }
}
