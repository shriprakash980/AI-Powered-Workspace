package com.devpilot.ai.exception;

import com.devpilot.ai.context.ContextException;
import com.devpilot.ai.dto.ErrorResponse;
import com.devpilot.ai.patch.PatchConflictException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PatchConflictException.class)
    public ResponseEntity<ErrorResponse> handlePatchConflictException(PatchConflictException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .errorCode("CONCURRENCY_CONFLICT")
                .conflict(ex.getConflict())
                .path(request.getRequestURI())
                .timestamp(Instant.now().toString())
                .build();

        log.warn("Patch concurrency conflict on path {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(ContextException.class)
    public ResponseEntity<ErrorResponse> handleContextException(ContextException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .path(request.getRequestURI())
                .timestamp(Instant.now().toString())
                .build();

        log.warn("Context exception on path {}: [{}] {}", request.getRequestURI(), ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorResponse.ValidationError> validationErrors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.add(new ErrorResponse.ValidationError(fieldError.getField(), fieldError.getDefaultMessage()));
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message("Input validation failed")
                .path(request.getRequestURI())
                .timestamp(Instant.now().toString())
                .errors(validationErrors)
                .build();

        log.warn("Validation error on path {}: {}", request.getRequestURI(), validationErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now().toString())
                .build();

        log.warn("Resource not found on path {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(ConflictException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now().toString())
                .errors(List.of())
                .build();

        log.warn("Conflict on path {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now().toString())
                .build();

        log.warn("Bad request on path {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now().toString())
                .build();

        log.warn("Unauthorized on path {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(ForbiddenException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now().toString())
                .build();

        log.warn("Forbidden on path {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now().toString())
                .build();

        log.warn("Illegal argument on path {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message("You do not have permission to access this resource")
                .path(request.getRequestURI())
                .timestamp(Instant.now().toString())
                .build();

        log.warn("Access denied on path {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message("Authentication required or token expired")
                .path(request.getRequestURI())
                .timestamp(Instant.now().toString())
                .build();

        log.warn("Authentication failure on path {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(com.devpilot.ai.git.exception.GitConflictException.class)
    public ResponseEntity<ErrorResponse> handleGitConflictException(com.devpilot.ai.git.exception.GitConflictException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .errorCode(ex.getConflictType())
                .conflict(ex.getConflictingFiles())
                .path(request.getRequestURI())
                .timestamp(Instant.now().toString())
                .build();

        log.warn("Git conflict on path {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(com.devpilot.ai.git.exception.GitException.class)
    public ResponseEntity<ErrorResponse> handleGitException(com.devpilot.ai.git.exception.GitException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now().toString())
                .build();

        log.warn("Git error on path {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(com.devpilot.ai.git.exception.GitHubRateLimitException.class)
    public ResponseEntity<ErrorResponse> handleGitHubRateLimitException(com.devpilot.ai.git.exception.GitHubRateLimitException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now().toString())
                .build();

        log.warn("GitHub rate limited on path {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }

    @ExceptionHandler(com.devpilot.ai.git.exception.GitHubApiException.class)
    public ResponseEntity<ErrorResponse> handleGitHubApiException(com.devpilot.ai.git.exception.GitHubApiException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(Instant.now().toString())
                .build();

        log.warn("GitHub API error on path {}: status={}, code={}, message={}", request.getRequestURI(), ex.getStatusCode(), ex.getErrorCode(), ex.getMessage());
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode());
        if (status == null) status = HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message("An internal server error occurred. Please try again later.")
                .path(request.getRequestURI())
                .timestamp(Instant.now().toString())
                .build();

        log.error("Unhandled server exception on path {}: ", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
