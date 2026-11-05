package com.viajajunto.api.modules.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateUserDTOValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private Set<ConstraintViolation<UpdateUserDTO>> validate(String nome) {
        return validator.validate(UpdateUserDTO.builder().nome(nome).build());
    }

    @Test
    @DisplayName("Nome ausente (null) é aceito: no PATCH significa 'não alterar'")
    void nullNameIsAccepted() {
        assertTrue(validate(null).isEmpty());
    }

    @Test
    @DisplayName("Nome só com espaços é rejeitado com a mensagem de nome em branco")
    void blankNameIsRejected() {
        Set<ConstraintViolation<UpdateUserDTO>> violations = validate("   ");

        assertEquals(1, violations.size());
        assertEquals("O nome não pode ficar em branco.", violations.iterator().next().getMessage());
    }

    @Test
    @DisplayName("Nome vazio e nome só com tabulação ou quebra de linha também são rejeitados")
    void emptyAndWhitespaceOnlyNamesAreRejected() {
        assertEquals(1, validate("").size());
        assertEquals(1, validate("\t").size());
        assertEquals(1, validate(" \n ").size());
    }

    @Test
    @DisplayName("Nome com espaços nas pontas é aceito (o serviço faz o trim)")
    void nameWithSurroundingSpacesIsAccepted() {
        assertTrue(validate("  Ana  ").isEmpty());
        assertTrue(validate("Ana Maria da Silva").isEmpty());
        assertTrue(validate("A").isEmpty());
    }

    @Test
    @DisplayName("Nome com mais de 255 caracteres é rejeitado pelo @Size")
    void tooLongNameIsRejected() {
        assertEquals(1, validate("a".repeat(256)).size());
    }

    @Test
    @DisplayName("Entrada patológica (255 espaços) é validada rapidamente, sem travar")
    void pathologicalInputIsValidatedQuickly() {
        String onlySpaces = " ".repeat(255);
        String spacesThenLetter = " ".repeat(254) + "a";

        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
            assertEquals(1, validate(onlySpaces).size());
            assertTrue(validate(spacesThenLetter).isEmpty());
        });
    }
}
