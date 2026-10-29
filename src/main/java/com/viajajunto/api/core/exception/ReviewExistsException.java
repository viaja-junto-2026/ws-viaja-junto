package com.viajajunto.api.core.exception;

import org.springframework.http.HttpStatus;

public class ReviewExistsException extends BusinessRuleException {

    public ReviewExistsException(String message) {
        super("REVIEW_EXISTS", message, HttpStatus.CONFLICT);
    }

    public ReviewExistsException() {
        this("Já existe uma avaliação registrada para este local.");
    }
}
