package com.viajajunto.api.core.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    @DisplayName("handleGenericException deve retornar 500 com code INTERNAL_SERVER_ERROR")
    void handleGenericException_Returns500WithInternalServerErrorCode() {
        Exception ex = new RuntimeException("Erro inesperado");

        ResponseEntity<ErrorResponseDTO> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getCode());
        assertFalse(response.getBody().getMessage().contains("Erro inesperado"));
    }

    private MethodParameter anyMethodParameter() throws Exception {
        return new MethodParameter(GlobalExceptionHandlerTest.class.getDeclaredMethod("setUp"), -1);
    }

    @Test
    @DisplayName("handleMalformedRequest deve retornar 400 MALFORMED_REQUEST sem expor o detalhe interno do parser")
    void handleMalformedRequest_Returns400WithoutInternalDetail() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error: Unexpected character (detalhe interno)", new MockHttpInputMessage(new byte[0]));

        ResponseEntity<ErrorResponseDTO> response = handler.handleMalformedRequest(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("MALFORMED_REQUEST", response.getBody().getCode());
        assertFalse(response.getBody().getMessage().contains("detalhe interno"));
    }

    @Test
    @DisplayName("handleMethodNotAllowed deve retornar 405 com o cabeçalho Allow dos métodos aceitos")
    void handleMethodNotAllowed_Returns405WithAllowHeader() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("DELETE", List.of("GET", "POST"));

        ResponseEntity<ErrorResponseDTO> response = handler.handleMethodNotAllowed(ex);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("METHOD_NOT_ALLOWED", response.getBody().getCode());
        assertEquals(Set.of(HttpMethod.GET, HttpMethod.POST), Set.copyOf(response.getHeaders().getAllow()));
    }

    @Test
    @DisplayName("handleMethodNotAllowed sem lista de métodos aceitos responde 405 sem o cabeçalho Allow")
    void handleMethodNotAllowed_WithoutSupportedMethods_OmitsAllowHeader() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("DELETE");

        ResponseEntity<ErrorResponseDTO> response = handler.handleMethodNotAllowed(ex);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertTrue(response.getHeaders().getAllow().isEmpty());
    }

    @Test
    @DisplayName("handleUnsupportedMediaType deve retornar 415 UNSUPPORTED_MEDIA_TYPE")
    void handleUnsupportedMediaType_Returns415() {
        HttpMediaTypeNotSupportedException ex =
                new HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<ErrorResponseDTO> response = handler.handleUnsupportedMediaType(ex);

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UNSUPPORTED_MEDIA_TYPE", response.getBody().getCode());
    }

    @Test
    @DisplayName("handleTypeMismatch deve retornar 400 INVALID_PARAMETER com o parâmetro e o tipo esperado")
    void handleTypeMismatch_Returns400WithFieldAndExpectedType() throws Exception {
        MethodArgumentTypeMismatchException ex =
                new MethodArgumentTypeMismatchException("abc", Integer.class, "page", anyMethodParameter(), null);

        ResponseEntity<ErrorResponseDTO> response = handler.handleTypeMismatch(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_PARAMETER", response.getBody().getCode());
        assertEquals("page", response.getBody().getDetails().get(0).getField());
        assertTrue(response.getBody().getDetails().get(0).getIssue().contains("Integer"));
    }

    @Test
    @DisplayName("handleTypeMismatch sem tipo esperado informa só que o valor é inválido")
    void handleTypeMismatch_WithoutRequiredType_ReportsInvalidValue() throws Exception {
        MethodArgumentTypeMismatchException ex =
                new MethodArgumentTypeMismatchException("abc", null, "limit", anyMethodParameter(), null);

        ResponseEntity<ErrorResponseDTO> response = handler.handleTypeMismatch(ex);

        assertEquals("valor inválido", response.getBody().getDetails().get(0).getIssue());
    }

    @Test
    @DisplayName("handleMissingParameter deve retornar 400 INVALID_PARAMETER com o nome do parâmetro ausente")
    void handleMissingParameter_Returns400WithParameterName() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("q", "String");

        ResponseEntity<ErrorResponseDTO> response = handler.handleMissingParameter(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_PARAMETER", response.getBody().getCode());
        assertEquals("q", response.getBody().getDetails().get(0).getField());
    }

    @Test
    @DisplayName("handleConstraintViolation deve retornar 422 VALIDATION_ERROR só com o nome do campo, sem o caminho do método")
    void handleConstraintViolation_Returns422WithFieldName() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("listTrips.page");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("deve ser maior que 0");
        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ErrorResponseDTO> response = handler.handleConstraintViolation(ex);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().getCode());
        assertEquals("page", response.getBody().getDetails().get(0).getField());
        assertEquals("deve ser maior que 0", response.getBody().getDetails().get(0).getIssue());
    }

    @Test
    @DisplayName("handleConstraintViolation mantém o nome do campo quando o caminho não tem ponto")
    void handleConstraintViolation_PathWithoutDot_KeepsFieldName() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("limit");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("inválido");

        ResponseEntity<ErrorResponseDTO> response =
                handler.handleConstraintViolation(new ConstraintViolationException(Set.of(violation)));

        assertEquals("limit", response.getBody().getDetails().get(0).getField());
    }
}
