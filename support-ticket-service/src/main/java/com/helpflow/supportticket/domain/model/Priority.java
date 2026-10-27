package com.helpflow.supportticket.domain.model;

public enum Priority {
    BAJA, MEDIA, ALTA, CRITICA;

    /**
     * Next priority level; the highest level stays unchanged.
     */
    public Priority escalate() {
        return this == CRITICA ? this : values()[ordinal() + 1];
    }
}
