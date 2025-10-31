package org.transactions.digitalwallettraining.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TransactionAmountValidator implements ConstraintValidator<ValidTransactionAmount, Double> {

    private static final double MAX_AMOUNT = 100_000;

    @Override
    public boolean isValid(Double amount, ConstraintValidatorContext context) {
        if (amount == null) return true; // let @NotNull handle it

        if (amount <= 0) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Transaction amount must be greater than zero")
                    .addConstraintViolation();
            return false;
        }

        if (amount > MAX_AMOUNT) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Transaction amount cannot exceed " + MAX_AMOUNT
            ).addConstraintViolation();
            return false;
        }

        return true;
    }
}
