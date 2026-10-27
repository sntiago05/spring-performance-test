package com.helpflow.supportticket.support;

import com.helpflow.supportticket.domain.model.Actor;
import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.Priority;
import com.helpflow.supportticket.domain.model.Role;
import com.helpflow.supportticket.domain.model.SlaPolicy;
import com.helpflow.supportticket.domain.model.Technician;
import com.helpflow.supportticket.domain.model.Ticket;
import com.helpflow.supportticket.domain.model.User;
import com.helpflow.supportticket.domain.model.UserStatus;
import java.time.Instant;

/**
 * Small builders for domain objects used across unit tests.
 */
public final class Fixtures {

    public static final Instant NOW = Instant.parse("2026-03-10T12:00:00Z");

    private Fixtures() {
    }

    public static Technician technician(long id, Category specialty, String site, int max) {
        return new Technician(id, "TEC-%03d".formatted(id), 100 + id, "Tech " + id, specialty, site, max,
                UserStatus.ACTIVO);
    }

    public static Technician inactive(Technician technician) {
        return technician.withProfile(technician.fullName(), technician.specialty(), technician.site(),
                technician.maxActiveTickets(), UserStatus.INACTIVO);
    }

    public static SlaPolicy sla(Priority priority, int hours) {
        return new SlaPolicy(priority, hours, 80);
    }

    public static Ticket openTicket(long id, Category category, Priority priority, String site) {
        Ticket opened = Ticket.open(1L, "Title of ticket", "Description of ticket", category, priority, site,
                "ASSET", NOW, sla(priority, 8));
        return new Ticket(id, opened.requesterId(), opened.title(), opened.description(), opened.category(),
                opened.priority(), opened.site(), opened.affectedAsset(), opened.status(), opened.createdAt(),
                opened.slaDueAt(), null, null, null, false, null, false, 0L);
    }

    public static User user(long id, UserStatus status, Role role) {
        return new User(id, "DOC" + id, "User " + id, "user" + id + "@helpflow.example", "hash", "BOGOTA",
                "Finance", status, role);
    }

    public static Actor actor(long userId, Role role) {
        return new Actor(userId, "user" + userId + "@helpflow.example", role);
    }
}
