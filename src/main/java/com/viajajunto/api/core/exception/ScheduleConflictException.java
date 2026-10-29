package com.viajajunto.api.core.exception;

import org.springframework.http.HttpStatus;

public class ScheduleConflictException extends BusinessRuleException {

    public ScheduleConflictException(String message) {
        super("SCHEDULE_CONFLICT", message, HttpStatus.CONFLICT);
    }

    public ScheduleConflictException() {
        this("Conflito de horário com outra atividade agendada.");
    }
}
