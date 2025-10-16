package org.transactions.digitalwallettraining.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ErrorResponseDTO {

    private LocalDateTime timestamp;
    private int status;           // HTTP status code
    private String error;         // General error message
    private List<FieldError> fieldErrors; // Field-specific validation errors

    // Nested class for individual field errors
    public static class FieldError {
        private String field;
        private String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() { return field; }
        public String getMessage() { return message; }
    }

    public ErrorResponseDTO() {}

    public ErrorResponseDTO(LocalDateTime timestamp, int status, String error, List<FieldError> fieldErrors) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.fieldErrors = fieldErrors;
    }

    // Getters and setters
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public List<FieldError> getFieldErrors() { return fieldErrors; }
    public void setFieldErrors(List<FieldError> fieldErrors) { this.fieldErrors = fieldErrors; }
}
