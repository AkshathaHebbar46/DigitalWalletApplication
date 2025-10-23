package org.transactions.digitalwallettraining.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TransactionAmountValidator implements ConstraintValidator<ValidTransactionAmount, Double> {

    private static final double MAX_AMOUNT = 100_000;

    @Override
    public boolean isValid(Double amount, ConstraintValidatorContext context) {
        if (amount == null) return true; // let @NotNull handle it
        return amount > 0 && amount <= MAX_AMOUNT;
    }
}
