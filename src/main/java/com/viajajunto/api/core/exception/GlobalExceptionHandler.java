package com.viajajunto.api.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFound(ResourceNotFoundException ex) {
        return notFound(ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNoResourceFound(NoResourceFoundException ex) {
        return notFound("Recurso não encontrado");
    }

    // Cobre também ScheduleConflictException, ReviewExistsException e EmailInUseException,
    // que carregam o próprio code e status (409).
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusinessRule(BusinessRuleException ex) {
        HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.BAD_REQUEST;
        String code = ex.getCode() != null ? ex.getCode() : "BUSINESS_RULE_VIOLATION";
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .code(code)
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler({UnauthorizedAccessException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResponseDTO> handleUnauthorized(RuntimeException ex) {
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .code("FORBIDDEN")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDTO> handleBadCredentials(BadCredentialsException ex) {
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .code("UNAUTHORIZED")
                .message("Credenciais inválidas: e-mail ou senha incorretos.")
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<ErrorDetail> details = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    String fieldName = (error instanceof FieldError fe) ? fe.getField() : error.getObjectName();
                    String errorMessage = error.getDefaultMessage();
                    return ErrorDetail.builder()
                            .field(fieldName)
                            .issue(errorMessage)
                            .build();
                })
                .toList();

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .code("VALIDATION_ERROR")
                .message("Dados fornecidos são inválidos")
                .details(details)
                .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex) {
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .code("INTERNAL_SERVER_ERROR")
                .message("Ocorreu um erro interno inesperado no servidor.")
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private ResponseEntity<ErrorResponseDTO> notFound(String message) {
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .code("NOT_FOUND")
                .message(message != null ? message : "Recurso não encontrado")
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
