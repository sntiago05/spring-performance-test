package com.helpflow.supportticket.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.databind.JsonNode;
import com.helpflow.supportticket.support.AbstractIntegrationTest;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * SLA monitoring with a controllable clock: escalation near the deadline, breach flagging, overdue
 * filter, configuration by admins and the related metrics.
 */
class SlaEscalationIT extends AbstractIntegrationTest {

    private JsonNode runReview(String admin) throws Exception {
        MvcResult result = postJson("/tickets/escalations", admin, Map.of());
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return json(result);
    }

    private String prometheus() throws Exception {
        return mockMvc.perform(get("/actuator/prometheus")).andReturn().getResponse().getContentAsString();
    }

    @Test
    void aTicketCloseToItsDeadlineIsEscalatedOnceAndRecordedInTheHistory() throws Exception {
        String user = newUserToken();
        String admin = adminToken();
        JsonNode ticket = createTicket(user, "RED", "MEDIA", "BOGOTA"); // 24 h window, threshold 80% = 19.2 h
        long id = ticket.get("id").asLong();

        clock.advance(Duration.ofHours(10));
        JsonNode early = runReview(admin);
        clock.advance(Duration.ofHours(10)); // 20 h elapsed
        JsonNode due = runReview(admin);
        JsonNode again = runReview(admin);

        assertThat(early.get("escalated").asInt()).isZero();
        assertThat(due.get("escalated").asInt()).isEqualTo(1);
        assertThat(due.get("breached").asInt()).isZero();
        assertThat(again.get("escalated").asInt()).isZero(); // idempotent
        JsonNode escalated = json(getJson("/tickets/" + id, user));
        assertThat(escalated.get("priority").asText()).isEqualTo("ALTA");
        assertThat(escalated.get("escalated").asBoolean()).isTrue();
        assertThat(escalated.get("slaBreached").asBoolean()).isFalse();
        assertThat(escalated.get("slaDueAt").asText()).isEqualTo(ticket.get("slaDueAt").asText());
        JsonNode history = json(getJson("/tickets/" + id + "/history", user));
        assertThat(history.findValuesAsText("action")).containsExactly("CREATED", "ASSIGNED", "ESCALATED");
        assertThat(history.get(2).get("performedBy").asText()).isEqualTo("SYSTEM");
        assertThat(history.get(2).get("observations").asText()).contains("MEDIA -> ALTA");
    }

    @Test
    void ticketsPastTheirDeadlineAreFlaggedAsBreachedAndListedAsOverdue() throws Exception {
        String user = newUserToken();
        String admin = adminToken();
        long late = createTicket(user, "RED", "ALTA", "BOGOTA").get("id").asLong(); // 8 h
        long onTime = createTicket(user, "OTRO", "BAJA", "BOGOTA").get("id").asLong(); // 72 h

        clock.advance(Duration.ofHours(9));
        JsonNode summary = runReview(admin);
        JsonNode overdue = json(getJson("/tickets?overdue=true", admin));

        assertThat(summary.get("breached").asInt()).isEqualTo(1);
        assertThat(overdue.get("totalElements").asInt()).isEqualTo(1);
        assertThat(overdue.get("content").get(0).get("id").asLong()).isEqualTo(late);
        assertThat(overdue.get("content").get(0).get("slaBreached").asBoolean()).isTrue();
        assertThat(json(getJson("/tickets/" + onTime, user)).get("slaBreached").asBoolean()).isFalse();
        JsonNode history = json(getJson("/tickets/" + late + "/history", user));
        assertThat(history.findValuesAsText("action")).contains("ESCALATED", "SLA_BREACHED");
        // running the review again does not flag it a second time
        assertThat(runReview(admin).get("breached").asInt()).isZero();
    }

    @Test
    void resolvingAfterTheDeadlineFlagsTheBreachAndStopsTheTicketFromBeingOverdue() throws Exception {
        suggest("TEC-008", 2);
        String user = newUserToken();
        String admin = adminToken();
        String technician = technicianToken("TEC-008");
        long id = createTicket(user, "RED", "ALTA", "MEDELLIN").get("id").asLong();
        patchJson("/tickets/" + id + "/status", technician, Map.of("status", "EN_PROGRESO", "observations", "Working"));

        clock.advance(Duration.ofHours(9));
        MvcResult resolved = patchJson("/tickets/" + id + "/status", technician,
                Map.of("status", "RESUELTO", "observations", "Finally solved"));

        assertThat(json(resolved).get("slaBreached").asBoolean()).isTrue();
        assertThat(json(getJson("/tickets?overdue=true", admin)).get("totalElements").asInt()).isZero();
        assertThat(json(getJson("/tickets/" + id + "/history", user)).findValuesAsText("action"))
                .contains("SLA_BREACHED");
    }

    @Test
    void finishedTicketsAreNeverEscalatedOrFlagged() throws Exception {
        String admin = adminToken();
        long id = createTicket(newUserToken(), "RED", "ALTA", "BOGOTA").get("id").asLong();
        patchJson("/tickets/" + id + "/status", admin,
                Map.of("status", "CANCELADO", "observations", "Cancelled by the administrator"));

        clock.advance(Duration.ofDays(5));

        assertThat(runReview(admin).get("reviewed").asInt()).isZero();
    }

    @Test
    void adminsConfigureTheSlaAndItAppliesToNewTicketsOnly() throws Exception {
        String user = newUserToken();
        String admin = adminToken();
        JsonNode before = createTicket(user, "RED", "ALTA", "BOGOTA");

        MvcResult updated = putJson("/sla-policies/ALTA", admin,
                Map.of("resolutionHours", 2, "escalationThresholdPercent", 50));
        JsonNode after = createTicket(user, "RED", "ALTA", "BOGOTA");

        assertThat(updated.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(updated).get("resolutionHours").asInt()).isEqualTo(2);
        assertThat(Duration.between(java.time.Instant.parse(before.get("createdAt").asText()),
                java.time.Instant.parse(before.get("slaDueAt").asText()))).isEqualTo(Duration.ofHours(8));
        assertThat(Duration.between(java.time.Instant.parse(after.get("createdAt").asText()),
                java.time.Instant.parse(after.get("slaDueAt").asText()))).isEqualTo(Duration.ofHours(2));
        // the new 50% threshold escalates the new ticket after 1 hour
        clock.advance(Duration.ofHours(1));
        runReview(admin);
        assertThat(json(getJson("/tickets/" + after.get("id").asLong(), admin)).get("escalated").asBoolean()).isTrue();
        assertThat(json(getJson("/tickets/" + before.get("id").asLong(), admin)).get("escalated").asBoolean()).isFalse();
    }

    @Test
    void invalidSlaConfigurationIsRejected() throws Exception {
        String admin = adminToken();

        MvcResult zeroHours = putJson("/sla-policies/ALTA", admin,
                Map.of("resolutionHours", 0, "escalationThresholdPercent", 80));
        MvcResult hugeThreshold = putJson("/sla-policies/ALTA", admin,
                Map.of("resolutionHours", 8, "escalationThresholdPercent", 100));
        MvcResult unknownPriority = putJson("/sla-policies/URGENTE", admin,
                Map.of("resolutionHours", 8, "escalationThresholdPercent", 80));

        assertThat(zeroHours.getResponse().getStatus()).isEqualTo(400);
        assertThat(hugeThreshold.getResponse().getStatus()).isEqualTo(400);
        assertThat(unknownPriority.getResponse().getStatus()).isEqualTo(400);
        assertThat(json(unknownPriority).get("type").asText()).endsWith("invalid-parameter");
    }

    @Test
    void businessMetricsAreExposedToPrometheus() throws Exception {
        suggest("TEC-008", 2);
        String user = newUserToken();
        String admin = adminToken();
        String technician = technicianToken("TEC-008");
        long id = createTicket(user, "RED", "ALTA", "MEDELLIN").get("id").asLong();
        patchJson("/tickets/" + id + "/status", technician, Map.of("status", "EN_PROGRESO", "observations", "Start"));
        patchJson("/tickets/" + id + "/status", technician, Map.of("status", "RESUELTO", "observations", "Done"));
        createTicket(user, "SOFTWARE", "BAJA", "CALI");
        clock.advance(Duration.ofHours(80));
        runReview(admin);

        String metrics = prometheus();

        assertThat(metrics).contains("application=\"support-ticket-service\"");
        assertThat(metrics).containsPattern("helpflow_tickets_registered_total\\{[^}]*category=\"RED\"[^}]*priority=\"ALTA\"[^}]*\\} [1-9]");
        assertThat(metrics).containsPattern("helpflow_tickets_registered_total\\{[^}]*category=\"SOFTWARE\"[^}]*priority=\"BAJA\"[^}]*\\} [1-9]");
        assertThat(metrics).containsPattern("helpflow_tickets_first_response_seconds_count\\{[^}]*\\} [1-9]");
        assertThat(metrics).containsPattern("helpflow_tickets_resolution_seconds_sum\\{[^}]*\\} ");
        assertThat(metrics).containsPattern("helpflow_tickets_escalated_total\\{[^}]*\\} [1-9]");
        assertThat(metrics).containsPattern("helpflow_tickets_sla_breached_total\\{[^}]*\\} [1-9]");
        assertThat(metrics).containsPattern("helpflow_tickets_overdue\\{[^}]*\\} [1-9]");
        assertThat(metrics).containsPattern("helpflow_tickets_assigned_total\\{[^}]*source=\"DISPATCH\"[^}]*\\} [1-9]");
    }
}
