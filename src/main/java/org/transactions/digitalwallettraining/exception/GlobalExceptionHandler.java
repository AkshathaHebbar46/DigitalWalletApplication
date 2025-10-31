package org.transactions.digitalwallettraining.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.transactions.digitalwallettraining.dto.ErrorResponseDTO;

import jakarta.persistence.OptimisticLockException;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 🧩 Validation Errors (@NotNull, @Min, etc.)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationExceptions(MethodArgumentNotValidException ex) {
        logger.warn("Validation failed: {}", ex.getMessage());

        var fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> new ErrorResponseDTO.FieldError(err.getField(), err.getDefaultMessage()))
                .collect(Collectors.toList());

        ErrorResponseDTO response = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                fieldErrors
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 🔁 Optimistic Lock (Concurrent update conflict)
     */
    @ExceptionHandler({OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<ErrorResponseDTO> handleOptimisticLockException(Exception ex) {
        logger.warn("🔁 Optimistic lock detected: {}", ex.getMessage());

        ErrorResponseDTO response = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Concurrent update conflict. Please retry your transaction.",
                null
        );

        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    /**
     * 🔒 Database Deadlocks or Lock Acquisition Issues
     */
    @ExceptionHandler(CannotAcquireLockException.class)
    public ResponseEntity<ErrorResponseDTO> handleDeadlock(CannotAcquireLockException ex) {
        logger.warn("🔒 Deadlock detected: {}", ex.getMessage());

        ErrorResponseDTO response = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Database deadlock occurred. Please retry the transaction.",
                null
        );

        return new ResponseEntity<>(response, HttpStatus.SERVICE_UNAVAILABLE);
    }

    /**
     * ⚠️ SQL Integrity Constraint Violation
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrity(DataIntegrityViolationException ex) {
        logger.warn("⚠️ Data integrity violation: {}", ex.getMostSpecificCause().getMessage());

        ErrorResponseDTO response = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Data integrity violation: " + ex.getMostSpecificCause().getMessage(),
                null
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * ⚠️ Invalid Input or Business Logic Error
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(IllegalArgumentException ex) {
        logger.warn("⚠️ {}", ex.getMessage());

        ErrorResponseDTO response = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                null
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 🚫 Wallet Frozen / Temporary Unavailable
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalState(IllegalStateException ex) {
        logger.warn("🚫 {}", ex.getMessage());

        ErrorResponseDTO response = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(), // 403 Forbidden
                ex.getMessage(),
                null
        );

        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    /**
     * 🔁 Max Retry Attempts Reached (Custom exception)
     */
    @ExceptionHandler(MaxRetryExceededException.class)
    public ResponseEntity<ErrorResponseDTO> handleMaxRetryExceeded(MaxRetryExceededException ex) {
        logger.error("Max retry attempts exceeded: {}", ex.getMessage());

        ErrorResponseDTO response = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Max retry attempts exceeded due to concurrent modification. Please retry the request.",
                null
        );

        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    /**
     * 💥 Generic Unhandled Exception
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneric(Exception ex) {
        logger.error("💥 Unexpected error: {}", ex.getMessage());

        ErrorResponseDTO response = new ErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Something went wrong. Please try again later.",
                null
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
