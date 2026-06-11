package com.kuky.backend.config;

import com.kuky.backend.auth.exception.AccountNotActivatedException;
import com.kuky.backend.auth.exception.AuthException;
import com.kuky.backend.auth.exception.DuplicateEmailException;
import com.kuky.backend.auth.exception.DuplicateUsernameException;
import com.kuky.backend.auth.exception.InvalidTokenException;
import com.kuky.backend.auth.exception.RateLimitException;
import com.kuky.backend.admin.exception.StudentNotFoundException;
import com.kuky.backend.learning.exception.AssignmentNotFoundException;
import com.kuky.backend.learning.exception.SubmissionNotAllowedException;
import com.kuky.backend.presentations.exception.InvalidImageException;
import com.kuky.backend.presentations.exception.PresentationNotFoundException;
import com.kuky.backend.resources.exception.AlreadyOwnedException;
import com.kuky.backend.resources.exception.NotPurchasableException;
import com.kuky.backend.resources.exception.ResourceLockedException;
import com.kuky.backend.resources.exception.ResourceNotFoundException;
import com.kuky.backend.scheduling.exception.BookingNotAllowedException;
import com.kuky.backend.scheduling.exception.BookingNotFoundException;
import com.kuky.backend.scheduling.exception.MeetingProvisioningException;
import com.kuky.backend.scheduling.exception.SlotUnavailableException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Error de validación.");
        return ResponseEntity.badRequest()
                .body(Map.of("error", "VALIDATION_ERROR", "message", message));
    }

    /** Semantic validation failures raised by services/controllers (e.g. end before start). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.unprocessableEntity()
                .body(Map.of("error", "VALIDATION_ERROR", "message", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateEmail(DuplicateEmailException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "EMAIL_ALREADY_EXISTS", "message", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateUsername(DuplicateUsernameException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "USERNAME_ALREADY_EXISTS", "message", ex.getMessage()));
    }

    @ExceptionHandler(AccountNotActivatedException.class)
    public ResponseEntity<Map<String, String>> handleNotActivated(AccountNotActivatedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "ACCOUNT_NOT_ACTIVATED", "message", ex.getMessage()));
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, String>> handleAuth(AuthException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "INVALID_CREDENTIALS", "message", ex.getMessage()));
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<Map<String, String>> handleRateLimit(RateLimitException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", "RATE_LIMIT_EXCEEDED", "message", ex.getMessage()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidToken(InvalidTokenException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "INVALID_OR_EXPIRED_TOKEN", "message", ex.getMessage()));
    }

    // Scheduling exceptions

    @ExceptionHandler(SlotUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleSlotUnavailable(SlotUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "SLOT_UNAVAILABLE", "message", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateKey(DuplicateKeyException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "SLOT_UNAVAILABLE", "message", "Esta hora ya ha sido reservada."));
    }

    @ExceptionHandler(BookingNotAllowedException.class)
    public ResponseEntity<Map<String, String>> handleBookingNotAllowed(BookingNotAllowedException ex) {
        return switch (ex.getReason()) {
            case RANGE -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("error", "SLOT_OUT_OF_RANGE", "message", "Esta hora no está disponible para reservar."));
            case LEAD -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("error", "BOOKING_TOO_SOON", "message", "Reserva con al menos 24 horas de antelación."));
            case CUTOFF -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("error", "CANCELLATION_TOO_LATE", "message", "El plazo de cancelación ha pasado."));
            case STATE -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "ALREADY_CANCELLED", "message", "Esta reserva ya fue cancelada."));
        };
    }

    @ExceptionHandler(MeetingProvisioningException.class)
    public ResponseEntity<Map<String, String>> handleMeetingProvisioning(MeetingProvisioningException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "MEETING_PROVISIONING_FAILED", "message", ex.getMessage()));
    }

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleBookingNotFound(BookingNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "BOOKING_NOT_FOUND", "message", ex.getMessage()));
    }

    // Resources exceptions

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "RESOURCE_NOT_FOUND", "message", ex.getMessage()));
    }

    @ExceptionHandler(ResourceLockedException.class)
    public ResponseEntity<Map<String, String>> handleResourceLocked(ResourceLockedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "RESOURCE_LOCKED", "message", ex.getMessage()));
    }

    @ExceptionHandler(AlreadyOwnedException.class)
    public ResponseEntity<Map<String, String>> handleAlreadyOwned(AlreadyOwnedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "ALREADY_OWNED", "message", ex.getMessage()));
    }

    @ExceptionHandler(NotPurchasableException.class)
    public ResponseEntity<Map<String, String>> handleNotPurchasable(NotPurchasableException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", "NOT_PURCHASABLE", "message", ex.getMessage()));
    }

    // Learning exceptions

    @ExceptionHandler(AssignmentNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleAssignmentNotFound(AssignmentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "ASSIGNMENT_NOT_FOUND", "message", ex.getMessage()));
    }

    @ExceptionHandler(SubmissionNotAllowedException.class)
    public ResponseEntity<Map<String, String>> handleSubmissionNotAllowed(SubmissionNotAllowedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "SUBMISSION_NOT_ALLOWED", "message", ex.getMessage()));
    }

    // Admin / backoffice exceptions

    @ExceptionHandler(StudentNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleStudentNotFound(StudentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "STUDENT_NOT_FOUND", "message", ex.getMessage()));
    }

    @ExceptionHandler(PresentationNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePresentationNotFound(PresentationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "PRESENTATION_NOT_FOUND", "message", ex.getMessage()));
    }

    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<Map<String, String>> handleInvalidImage(InvalidImageException ex) {
        return ResponseEntity.unprocessableEntity()
                .body(Map.of("error", "INVALID_IMAGE", "message", ex.getMessage()));
    }
}
