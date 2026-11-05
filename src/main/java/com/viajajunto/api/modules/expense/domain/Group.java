package com.viajajunto.api.modules.expense.domain;

import java.util.Collections;
import java.util.List;

public class Group {

    private final String id;
    private final String name;
    private final List<Participant> participants;

    public Group(String id, String name, List<Participant> participants) {
        this.id = id;
        this.name = name;
        this.participants = participants != null ? List.copyOf(participants) : Collections.emptyList();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Participant> getParticipants() {
        return participants;
    }
}

