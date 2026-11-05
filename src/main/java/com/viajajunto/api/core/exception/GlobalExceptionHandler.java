package com.viajajunto.api.core.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Set;

@Slf4j
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

    // Cobre também ScheduleConflictException e ReviewExistsException,
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

    // Erros do próprio Spring causados pelo cliente: respondem 4xx no formato padrão, sem expor detalhes internos.

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleMalformedRequest(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Corpo da requisição ausente ou malformado.", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED);
        Set<HttpMethod> allowed = ex.getSupportedHttpMethods();
        if (allowed != null && !allowed.isEmpty()) {
            response.allow(allowed.toArray(new HttpMethod[0]));
        }
        return response.body(ErrorResponseDTO.builder()
                .code("METHOD_NOT_ALLOWED")
                .message("Método HTTP não suportado para este recurso.")
                .build());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "Tipo de conteúdo não suportado. Use application/json.", null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String issue = ex.getRequiredType() != null
                ? "valor inválido; esperado: " + ex.getRequiredType().getSimpleName()
                : "valor inválido";
        return error(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "Parâmetro inválido.",
                List.of(ErrorDetail.builder().field(ex.getName()).issue(issue).build()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDTO> handleMissingParameter(MissingServletRequestParameterException ex) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "Parâmetro inválido.",
                List.of(ErrorDetail.builder().field(ex.getParameterName()).issue("parâmetro obrigatório ausente").build()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleConstraintViolation(ConstraintViolationException ex) {
        List<ErrorDetail> details = ex.getConstraintViolations().stream()
                .map(violation -> ErrorDetail.builder()
                        .field(lastNode(violation))
                        .issue(violation.getMessage())
                        .build())
                .toList();
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "Dados fornecidos são inválidos", details);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex) {
        // Sem o log, um erro 500 some sem deixar rastro da causa.
        log.error("Erro inesperado ao processar a requisição", ex);
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .code("INTERNAL_SERVER_ERROR")
                .message("Ocorreu um erro interno inesperado no servidor.")
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private ResponseEntity<ErrorResponseDTO> error(HttpStatus status, String code, String message, List<ErrorDetail> details) {
        return ResponseEntity.status(status).body(ErrorResponseDTO.builder()
                .code(code)
                .message(message)
                .details(details)
                .build());
    }

    // "listTrips.page" vira "page": só o nome do campo, sem o caminho interno do método.
    private String lastNode(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        return path.substring(path.lastIndexOf('.') + 1);
    }

    private ResponseEntity<ErrorResponseDTO> notFound(String message) {
        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .code("NOT_FOUND")
                .message(message != null ? message : "Recurso não encontrado")
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
