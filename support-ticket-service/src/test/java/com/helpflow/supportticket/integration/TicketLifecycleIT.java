package com.helpflow.supportticket.integration;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.JsonNode;
import com.helpflow.supportticket.support.AbstractIntegrationTest;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Specification flow: register -> SLA + dispatch -> assignment rules -> history -> transitions.
 */
class TicketLifecycleIT extends AbstractIntegrationTest {

    @Test
    void aNewTicketIsAssignedToTheSuggestedTechnicianWithItsSlaDeadlineAndHistory() throws Exception {
        suggest("TEC-008", 4); // RED technician in MEDELLIN, estimate fits the 8 h SLA of ALTA
        String user = newUserToken();

        JsonNode ticket = createTicket(user, "RED", "ALTA", "BOGOTA");

        assertThat(ticket.get("status").asString()).isEqualTo("ASIGNADO");
        assertThat(ticket.get("technician").get("code").asString()).isEqualTo("TEC-008");
        assertThat(ticket.get("requester").get("fullName").asString()).startsWith("User ");
        Instant created = Instant.parse(ticket.get("createdAt").asString());
        Instant due = Instant.parse(ticket.get("slaDueAt").asString());
        assertThat(Duration.between(created, due)).isEqualTo(Duration.ofHours(8));

        long id = ticket.get("id").asLong();
        JsonNode assignments = json(getJson("/tickets/" + id + "/assignments", user));
        assertThat(assignments).hasSize(1);
        assertThat(assignments.get(0).get("source").asString()).isEqualTo("DISPATCH");
        assertThat(assignments.get(0).get("estimatedHours").asInt()).isEqualTo(4);
        assertThat(assignments.get(0).get("withinSla").asBoolean()).isTrue();
        assertThat(assignments.get(0).get("active").asBoolean()).isTrue();

        JsonNode history = json(getJson("/tickets/" + id + "/history", user));
        assertThat(history.findValuesAsString("action")).containsExactly("CREATED", "ASSIGNED");
        assertThat(history.get(0).get("performedBy").asString()).startsWith("user.");
        assertThat(history.get(1).get("performedBy").asString()).isEqualTo("SYSTEM");
    }

    @Test
    void theSlaWindowFollowsThePriority() throws Exception {
        String user = newUserToken();
        long[] expectedHours = {72, 24, 8, 4};
        String[] priorities = {"BAJA", "MEDIA", "ALTA", "CRITICA"};

        for (int i = 0; i < priorities.length; i++) {
            JsonNode ticket = createTicket(user, "OTRO", priorities[i], "BOGOTA");
            Duration window = Duration.between(Instant.parse(ticket.get("createdAt").asString()),
                    Instant.parse(ticket.get("slaDueAt").asString()));
            assertThat(window).as(priorities[i]).isEqualTo(Duration.ofHours(expectedHours[i]));
        }
    }

    @Test
    void theTechnicianWorksTheTicketThroughItsWholeLifecycleLeavingAnAuditTrail() throws Exception {
        suggest("TEC-008", 3);
        String user = newUserToken();
        long id = createTicket(user, "RED", "MEDIA", "MEDELLIN").get("id").asLong();
        String technician = technicianToken("TEC-008");
        String admin = adminToken();

        MvcResult started = patchJson("/tickets/" + id + "/status", technician,
                Map.of("status", "EN_PROGRESO", "observations", "Checking the access point"));
        MvcResult resolved = patchJson("/tickets/" + id + "/status", technician,
                Map.of("status", "RESUELTO", "observations", "Firmware updated"));
        MvcResult closed = patchJson("/tickets/" + id + "/status", admin,
                Map.of("status", "CERRADO", "observations", "Confirmed by the user"));

        assertThat(started.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(started).get("status").asString()).isEqualTo("EN_PROGRESO");
        assertThat(json(started).get("firstResponseAt").isNull()).isFalse();
        assertThat(json(resolved).get("resolvedAt").isNull()).isFalse();
        assertThat(json(resolved).get("slaBreached").asBoolean()).isFalse();
        assertThat(json(closed).get("status").asString()).isEqualTo("CERRADO");

        JsonNode history = json(getJson("/tickets/" + id + "/history", user));
        assertThat(history.findValuesAsString("action"))
                .containsExactly("CREATED", "ASSIGNED", "STATUS_CHANGED", "STATUS_CHANGED", "STATUS_CHANGED");
        assertThat(history.get(2).get("fromStatus").asString()).isEqualTo("ASIGNADO");
        assertThat(history.get(2).get("toStatus").asString()).isEqualTo("EN_PROGRESO");
        assertThat(history.get(2).get("observations").asString()).isEqualTo("Checking the access point");
        assertThat(history.get(2).get("performedBy").asString()).isEqualTo("tec-008@helpflow.example");
    }

    @Test
    void invalidTransitionsAreConflictsAndDoNotChangeTheTicket() throws Exception {
        suggest("TEC-008", 3);
        long id = createTicket(newUserToken(), "RED", "MEDIA", "MEDELLIN").get("id").asLong();
        String technician = technicianToken("TEC-008");

        MvcResult skipped = patchJson("/tickets/" + id + "/status", technician,
                Map.of("status", "RESUELTO", "observations", "Skipping a step"));

        assertThat(skipped.getResponse().getStatus()).isEqualTo(409);
        JsonNode problem = json(skipped);
        assertThat(problem.get("type").asString()).isEqualTo("urn:helpflow:problem:invalid-transition");
        assertThat(problem.get("detail").asString()).contains("ASIGNADO", "RESUELTO");
        assertThat(json(getJson("/tickets/" + id, technician)).get("status").asString()).isEqualTo("ASIGNADO");
    }

    @Test
    void finalStatesAreFinalAndResolvedTicketsCanBeReopened() throws Exception {
        suggest("TEC-008", 3);
        long id = createTicket(newUserToken(), "RED", "MEDIA", "MEDELLIN").get("id").asLong();
        String technician = technicianToken("TEC-008");
        String admin = adminToken();
        patchJson("/tickets/" + id + "/status", technician, Map.of("status", "EN_PROGRESO", "observations", "Start"));
        patchJson("/tickets/" + id + "/status", technician, Map.of("status", "RESUELTO", "observations", "Done"));

        MvcResult reopened = patchJson("/tickets/" + id + "/status", technician,
                Map.of("status", "EN_PROGRESO", "observations", "The failure came back"));
        patchJson("/tickets/" + id + "/status", technician, Map.of("status", "RESUELTO", "observations", "Fixed again"));
        patchJson("/tickets/" + id + "/status", admin, Map.of("status", "CERRADO", "observations", "Closing"));
        MvcResult afterClose = patchJson("/tickets/" + id + "/status", admin,
                Map.of("status", "EN_PROGRESO", "observations", "Too late"));

        assertThat(reopened.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(reopened).get("resolvedAt").isNull()).isTrue();
        assertThat(afterClose.getResponse().getStatus()).isEqualTo(409);
    }

    @Test
    void requestValidationReturnsAProblemDocumentListingEveryInvalidField() throws Exception {
        String user = newUserToken();

        MvcResult result = postJson("/tickets", user, Map.of("title", "x", "description", "short",
                "category", "RED", "priority", "ALTA", "site", "", "affectedAsset", "AP"));

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        JsonNode problem = json(result);
        assertThat(problem.get("type").asString()).isEqualTo("urn:helpflow:problem:validation-error");
        assertThat(problem.get("errors").findValuesAsString("field")).contains("title", "description", "site");
        assertThat(problem.get("timestamp").asString()).isNotBlank();
        assertThat(problem.get("traceId").asString()).isEqualTo(result.getResponse().getHeader("X-Trace-Id"));
    }

    @Test
    void crossFieldRulesOfAStatusChangeAreValidated() throws Exception {
        suggest("TEC-008", 3);
        long id = createTicket(newUserToken(), "RED", "MEDIA", "MEDELLIN").get("id").asLong();
        String admin = adminToken();

        MvcResult toAssigned = patchJson("/tickets/" + id + "/status", admin,
                Map.of("status", "ASIGNADO", "observations", "Directly"));
        MvcResult shortCancel = patchJson("/tickets/" + id + "/status", admin,
                Map.of("status", "CANCELADO", "observations", "no"));
        MvcResult noObservations = patchJson("/tickets/" + id + "/status", admin, Map.of("status", "EN_PROGRESO"));
        MvcResult cancelled = patchJson("/tickets/" + id + "/status", admin,
                Map.of("status", "CANCELADO", "observations", "Duplicate of ticket 4, same asset"));

        assertThat(toAssigned.getResponse().getStatus()).isEqualTo(400);
        assertThat(json(toAssigned).get("errors").findValuesAsString("field")).contains("status");
        assertThat(shortCancel.getResponse().getStatus()).isEqualTo(400);
        assertThat(json(shortCancel).get("errors").findValuesAsString("field")).contains("observations");
        assertThat(noObservations.getResponse().getStatus()).isEqualTo(400);
        assertThat(cancelled.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(cancelled).get("status").asString()).isEqualTo("CANCELADO");
    }

    @Test
    void accentedAndLowercaseEnumValuesAreAccepted() throws Exception {
        String user = newUserToken();

        MvcResult result = postJson("/tickets", user, Map.of("title", "Server room on fire",
                "description", "Smoke coming out of the rack", "category", "hardware", "priority", "CRÍTICA",
                "site", "BOGOTA", "affectedAsset", "RACK-1"));

        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        assertThat(json(result).get("priority").asString()).isEqualTo("CRITICA");
    }

    @Test
    void inactiveRequestersCannotRegisterTickets() throws Exception {
        String suffix = uniqueSuffix();
        String email = "gone." + suffix + "@helpflow.example";
        long id = json(postJson("/auth/register", null, Map.of("document", "DOC-" + suffix, "fullName", "Gone",
                "email", email, "password", PASSWORD, "site", "BOGOTA", "area", "X"))).get("id").asLong();
        String token = login(email, PASSWORD);
        patchJson("/users/" + id + "/status", adminToken(), Map.of("status", "INACTIVO"));

        MvcResult result = postJson("/tickets", token, ticketBody("RED", "ALTA", "BOGOTA"));

        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        assertThat(json(result).get("type").asString()).isEqualTo("urn:helpflow:problem:user-inactive");
    }
}
