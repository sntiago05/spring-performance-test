package com.helpflow.supportticket.application.port.in;

public interface EscalateTicketsUseCase {

    /**
     * Reviews every ticket whose SLA is running: escalates those that consumed the configured share of
     * their window and flags those past their deadline.
     */
    Summary escalateDueTickets();

    record Summary(int reviewed, int escalated, int breached, int failed) {
    }
}
