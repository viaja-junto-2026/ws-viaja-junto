package com.viajajunto.api.modules.expense.domain;

public class Participant {
    private final String id;
    private final String name;

    public Participant(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }
}
