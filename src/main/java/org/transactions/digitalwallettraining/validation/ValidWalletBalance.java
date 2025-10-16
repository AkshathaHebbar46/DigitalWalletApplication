package org.transactions.digitalwallettraining.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = WalletBalanceValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidWalletBalance {
    String message() default "Invalid wallet balance";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
