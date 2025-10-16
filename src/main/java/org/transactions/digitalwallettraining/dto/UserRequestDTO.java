package org.transactions.digitalwallettraining.dto;

import jakarta.validation.constraints.*;

public record UserRequestDTO(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid") String email,
        @NotNull(message = "Age is required")
        @Min(value = 18, message = "Age must be at least 18")
        @Max(value = 100, message = "Age cannot exceed 100")
        Integer age

) {}
