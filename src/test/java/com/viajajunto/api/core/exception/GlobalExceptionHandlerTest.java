package com.viajajunto.api.core.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleNoResourceFound deve retornar 404 com mensagem fixa, sem expor detalhes internos do Spring")
    void handleNoResourceFound_Returns404WithFixedMessage() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/rota/inexistente");

        ResponseEntity<ErrorResponseDTO> response = handler.handleNoResourceFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("NOT_FOUND", response.getBody().getCode());
        assertEquals("Recurso não encontrado", response.getBody().getMessage());
    }

    @Test
    @DisplayName("handleValidationExceptions deve retornar 422 com code VALIDATION_ERROR e details")
    void handleValidationExceptions_Returns422WithDetails() throws Exception {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "registerDTO");
        bindingResult.addError(new FieldError("registerDTO", "email", "formato inválido"));
        bindingResult.addError(new FieldError("registerDTO", "password", "mínimo 6 caracteres"));

        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("setUp"), -1
        );
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorResponseDTO> response = handler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().getCode());
        assertEquals("Dados fornecidos são inválidos", response.getBody().getMessage());
        assertEquals(2, response.getBody().getDetails().size());
        assertEquals("email", response.getBody().getDetails().get(0).getField());
        assertEquals("formato inválido", response.getBody().getDetails().get(0).getIssue());
    }

    @Test
    @DisplayName("handleResourceNotFound deve retornar 404 com code NOT_FOUND")
    void handleResourceNotFound_Returns404WithNotFoundCode() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Item não encontrado");

        ResponseEntity<ErrorResponseDTO> response = handler.handleResourceNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("NOT_FOUND", response.getBody().getCode());
        assertEquals("Item não encontrado", response.getBody().getMessage());
    }

    @Test
    @DisplayName("handleScheduleConflict deve retornar 409 com code SCHEDULE_CONFLICT")
    void handleScheduleConflict_Returns409WithScheduleConflictCode() {
        ScheduleConflictException ex = new ScheduleConflictException("Conflito na agenda");

        ResponseEntity<ErrorResponseDTO> response = handler.handleBusinessRule(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SCHEDULE_CONFLICT", response.getBody().getCode());
        assertEquals("Conflito na agenda", response.getBody().getMessage());
    }

    @Test
    @DisplayName("handleReviewExists deve retornar 409 com code REVIEW_EXISTS")
    void handleReviewExists_Returns409WithReviewExistsCode() {
        ReviewExistsException ex = new ReviewExistsException("Avaliação já existe");

        ResponseEntity<ErrorResponseDTO> response = handler.handleBusinessRule(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("REVIEW_EXISTS", response.getBody().getCode());
        assertEquals("Avaliação já existe", response.getBody().getMessage());
    }

    @Test
    @DisplayName("handleEmailInUse deve retornar 409 com code EMAIL_IN_USE")
    void handleEmailInUse_Returns409WithEmailInUseCode() {
        EmailInUseException ex = new EmailInUseException("E-mail em uso");

        ResponseEntity<ErrorResponseDTO> response = handler.handleBusinessRule(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("EMAIL_IN_USE", response.getBody().getCode());
        assertEquals("E-mail em uso", response.getBody().getMessage());
    }

    @Test
    @DisplayName("handleBusinessRule deve retornar status e code configurados na exceção")
    void handleBusinessRule_ReturnsConfiguredStatusAndCode() {
        BusinessRuleException ex = new BusinessRuleException("Regra violada");

        ResponseEntity<ErrorResponseDTO> response = handler.handleBusinessRule(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("BUSINESS_RULE_VIOLATION", response.getBody().getCode());
        assertEquals("Regra violada", response.getBody().getMessage());
    }

    @Test
    @DisplayName("handleUnauthorized deve retornar 403 com code FORBIDDEN")
    void handleUnauthorized_Returns403WithForbiddenCode() {
        AccessDeniedException ex = new AccessDeniedException("Sem permissão");

        ResponseEntity<ErrorResponseDTO> response = handler.handleUnauthorized(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("FORBIDDEN", response.getBody().getCode());
    }

    @Test
    @DisplayName("handleBadCredentials deve retornar 401 com code UNAUTHORIZED")
    void handleBadCredentials_Returns401WithUnauthorizedCode() {
        BadCredentialsException ex = new BadCredentialsException("Bad creds");

        ResponseEntity<ErrorResponseDTO> response = handler.handleBadCredentials(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UNAUTHORIZED", response.getBody().getCode());
    }

    @Test
    @DisplayName("handleGenericException deve retornar 500 com code INTERNAL_SERVER_ERROR")
    void handleGenericException_Returns500WithInternalServerErrorCode() {
        Exception ex = new RuntimeException("Erro inesperado");

        ResponseEntity<ErrorResponseDTO> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getCode());
    }
}
