package com.viajajunto.api.core.exception;

import org.springframework.http.HttpStatus;

public class EmailInUseException extends BusinessRuleException {

    public EmailInUseException(String message) {
        super("EMAIL_IN_USE", message, HttpStatus.CONFLICT);
    }

    public EmailInUseException() {
        this("O e-mail informado já está em uso.");
    }
}
