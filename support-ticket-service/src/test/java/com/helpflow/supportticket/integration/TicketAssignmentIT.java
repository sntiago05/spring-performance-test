package com.helpflow.supportticket.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.helpflow.supportticket.support.AbstractIntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Assignment rules, manual (re)assignment and concurrency of the capacity limit.
 */
class TicketAssignmentIT extends AbstractIntegrationTest {

    private long technicianId(String code) {
        return jdbc.queryForObject("select id from technicians where code = ?", Long.class, code);
    }

    private JsonNode assignments(long ticketId, String token) throws Exception {
        return json(getJson("/tickets/" + ticketId + "/assignments", token));
    }

    @Test
    void whenDispatchIsDownTheRulesPickTheSameSiteSpecialistWithTheLowestWorkload() throws Exception {
        String user = newUserToken();

        JsonNode first = createTicket(user, "RED", "MEDIA", "BOGOTA");
        JsonNode second = createTicket(user, "RED", "MEDIA", "BOGOTA");

        // Same site and specialty tie at load 0 -> lowest code (TEC-007, RED/BOGOTA). The second ticket goes
        // to the generalist of the same site (TEC-013) because TEC-007 now carries one ticket.
        assertThat(first.get("technician").get("code").asText()).isEqualTo("TEC-007");
        assertThat(second.get("technician").get("code").asText()).isEqualTo("TEC-013");
        JsonNode source = assignments(first.get("id").asLong(), user);
        assertThat(source.get(0).get("source").asText()).isEqualTo("RULES");
        assertThat(source.get(0).get("estimatedHours").isNull()).isTrue();
        JsonNode history = json(getJson("/tickets/" + first.get("id").asLong() + "/history", user));
        assertThat(history.get(1).get("observations").asText()).contains("TEC-007", "RULES", "unavailable");
    }

    @Test
    void aSuggestionForAnIncompatibleTechnicianIsRejectedByTheRules() throws Exception {
        suggest("TEC-004", 1); // SOFTWARE technician for a RED ticket

        JsonNode ticket = createTicket(newUserToken(), "RED", "ALTA", "BOGOTA");

        assertThat(ticket.get("technician").get("code").asText()).isEqualTo("TEC-007");
        JsonNode history = json(getJson("/tickets/" + ticket.get("id").asLong() + "/history", adminToken()));
        assertThat(history.get(1).get("observations").asText()).contains("Dispatch suggestion TEC-004 rejected");
    }

    @Test
    void aSuggestionThatCannotMeetTheSlaIsNotUsed() throws Exception {
        suggest("TEC-008", 12); // 12 h estimate, but CRITICA tickets have a 4 h SLA

        JsonNode ticket = createTicket(newUserToken(), "RED", "CRITICA", "BOGOTA");

        assertThat(ticket.get("technician").get("code").asText()).isEqualTo("TEC-007");
        assertThat(assignments(ticket.get("id").asLong(), adminToken()).get(0).get("source").asText()).isEqualTo("RULES");
    }

    @Test
    void ticketsStayOpenWhenEveryEligibleTechnicianIsFullAndAnAdminCanAssignThemLater() throws Exception {
        // HARDWARE can be taken by TEC-001..003 and the generalists TEC-013..015: capacity 1 each
        jdbc.update("update technicians set max_active_tickets = 1 where code in "
                + "('TEC-001','TEC-002','TEC-003','TEC-013','TEC-014','TEC-015')");
        String user = newUserToken();
        List<JsonNode> tickets = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            tickets.add(createTicket(user, "HARDWARE", "MEDIA", "BOGOTA"));
        }

        long assigned = tickets.stream().filter(t -> t.get("status").asText().equals("ASIGNADO")).count();
        JsonNode pending = tickets.get(6);

        assertThat(assigned).isEqualTo(6);
        assertThat(pending.get("status").asText()).isEqualTo("ABIERTO");
        assertThat(pending.get("technician").isNull()).isTrue();
        JsonNode history = json(getJson("/tickets/" + pending.get("id").asLong() + "/history", user));
        assertThat(history.findValuesAsText("action")).containsExactly("CREATED", "ASSIGNMENT_PENDING");

        // the admin frees capacity and assigns the pending ticket manually
        jdbc.update("update technicians set max_active_tickets = 2 where code = 'TEC-001'");
        MvcResult manual = putJson("/tickets/" + pending.get("id").asLong() + "/assignment", adminToken(),
                Map.of("technicianId", technicianId("TEC-001"), "observations", "Manual assignment by admin"));

        assertThat(manual.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(manual).get("status").asText()).isEqualTo("ASIGNADO");
        assertThat(json(manual).get("technician").get("code").asText()).isEqualTo("TEC-001");
        JsonNode after = json(getJson("/tickets/" + pending.get("id").asLong() + "/history", user));
        assertThat(after.findValuesAsText("action")).containsExactly("CREATED", "ASSIGNMENT_PENDING", "ASSIGNED");
    }

    @Test
    void criticalTicketsUseTheOverflowSlotWhenEveryoneIsFull() throws Exception {
        jdbc.update("update technicians set max_active_tickets = 1 where code in "
                + "('TEC-001','TEC-002','TEC-003','TEC-013','TEC-014','TEC-015')");
        String user = newUserToken();
        for (int i = 0; i < 6; i++) {
            createTicket(user, "HARDWARE", "MEDIA", "BOGOTA");
        }

        JsonNode critical = createTicket(user, "HARDWARE", "CRITICA", "BOGOTA");

        assertThat(critical.get("status").asText()).isEqualTo("ASIGNADO");
    }

    @Test
    void reassignmentKeepsTheOldAssignmentAsInactiveAndRecordsTheHistory() throws Exception {
        String user = newUserToken();
        JsonNode ticket = createTicket(user, "RED", "MEDIA", "BOGOTA"); // TEC-007 by the rules
        long id = ticket.get("id").asLong();
        String admin = adminToken();

        MvcResult result = putJson("/tickets/" + id + "/assignment", admin,
                Map.of("technicianId", technicianId("TEC-009"), "observations", "Moved to the specialist in CALI"));

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(result).get("technician").get("code").asText()).isEqualTo("TEC-009");
        JsonNode all = assignments(id, admin);
        assertThat(all).hasSize(2);
        assertThat(all.get(0).get("active").asBoolean()).isFalse();
        assertThat(all.get(1).get("active").asBoolean()).isTrue();
        assertThat(all.get(1).get("source").asText()).isEqualTo("MANUAL");
        JsonNode history = json(getJson("/tickets/" + id + "/history", admin));
        assertThat(history.findValuesAsText("action")).containsExactly("CREATED", "ASSIGNED", "REASSIGNED");
        assertThat(history.get(2).get("observations").asText()).contains("TEC-009", "Moved to the specialist in CALI");
        // the previous technician no longer sees it, the new one does
        assertThat(json(getJson("/tickets", technicianToken("TEC-007"))).get("totalElements").asInt()).isZero();
        assertThat(json(getJson("/tickets", technicianToken("TEC-009"))).get("totalElements").asInt()).isEqualTo(1);
    }

    @Test
    void aReassignedTicketInProgressGoesBackToAssigned() throws Exception {
        long id = createTicket(newUserToken(), "RED", "MEDIA", "BOGOTA").get("id").asLong();
        patchJson("/tickets/" + id + "/status", technicianToken("TEC-007"),
                Map.of("status", "EN_PROGRESO", "observations", "Starting"));

        MvcResult result = putJson("/tickets/" + id + "/assignment", adminToken(),
                Map.of("technicianId", technicianId("TEC-009"), "observations", "Technician is on leave"));

        assertThat(json(result).get("status").asText()).isEqualTo("ASIGNADO");
    }

    @Test
    void invalidManualAssignmentsAreRejectedWithTheBusinessRuleThatFailed() throws Exception {
        long id = createTicket(newUserToken(), "RED", "MEDIA", "BOGOTA").get("id").asLong();
        String admin = adminToken();
        jdbc.update("update technicians set status = 'INACTIVO' where code = 'TEC-008'");
        jdbc.update("update technicians set max_active_tickets = 1 where code = 'TEC-009'");
        long fillerTicket = createTicket(newUserToken(), "OTRO", "BAJA", "CALI").get("id").asLong();
        putJson("/tickets/" + fillerTicket + "/assignment", admin,
                Map.of("technicianId", technicianId("TEC-009"), "observations", "Fill TEC-009 up to capacity"));

        MvcResult incompatible = putJson("/tickets/" + id + "/assignment", admin,
                Map.of("technicianId", technicianId("TEC-004"), "observations", "Wrong specialty"));
        MvcResult inactive = putJson("/tickets/" + id + "/assignment", admin,
                Map.of("technicianId", technicianId("TEC-008"), "observations", "Technician disabled"));
        MvcResult full = putJson("/tickets/" + id + "/assignment", admin,
                Map.of("technicianId", technicianId("TEC-009"), "observations", "Technician at capacity"));
        MvcResult same = putJson("/tickets/" + id + "/assignment", admin,
                Map.of("technicianId", technicianId("TEC-007"), "observations", "Already the owner"));
        MvcResult unknown = putJson("/tickets/" + id + "/assignment", admin,
                Map.of("technicianId", 987654, "observations", "Does not exist"));

        assertThat(incompatible.getResponse().getStatus()).isEqualTo(422);
        assertThat(json(incompatible).get("type").asText()).endsWith("technician-incompatible");
        assertThat(inactive.getResponse().getStatus()).isEqualTo(422);
        assertThat(json(inactive).get("type").asText()).endsWith("technician-inactive");
        assertThat(full.getResponse().getStatus()).isEqualTo(422);
        assertThat(json(full).get("type").asText()).endsWith("technician-at-capacity");
        assertThat(same.getResponse().getStatus()).isEqualTo(422);
        assertThat(json(same).get("type").asText()).endsWith("already-assigned");
        assertThat(unknown.getResponse().getStatus()).isEqualTo(404);
        assertThat(json(getJson("/tickets/" + id, admin)).get("technician").get("code").asText()).isEqualTo("TEC-007");
    }

    @Test
    void closedTicketsCannotBeAssigned() throws Exception {
        long id = createTicket(newUserToken(), "RED", "MEDIA", "BOGOTA").get("id").asLong();
        String admin = adminToken();
        patchJson("/tickets/" + id + "/status", admin,
                Map.of("status", "CANCELADO", "observations", "Cancelled, duplicate of another ticket"));

        MvcResult result = putJson("/tickets/" + id + "/assignment", admin,
                Map.of("technicianId", technicianId("TEC-009"), "observations", "Too late to assign"));

        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        assertThat(json(result).get("type").asText()).endsWith("ticket-not-assignable");
    }

    @Test
    void concurrentRegistrationsNeverExceedTheLastFreeSlot() throws Exception {
        // only TEC-001 can take hardware tickets, with room for exactly one
        jdbc.update("update technicians set status = 'INACTIVO' where code in "
                + "('TEC-002','TEC-003','TEC-013','TEC-014','TEC-015')");
        jdbc.update("update technicians set max_active_tickets = 1 where code = 'TEC-001'");
        String user = newUserToken();
        int requests = 6;

        ExecutorService pool = Executors.newFixedThreadPool(requests);
        List<Callable<JsonNode>> calls = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            calls.add(() -> createTicket(user, "HARDWARE", "MEDIA", "BOGOTA"));
        }
        List<JsonNode> results = new ArrayList<>();
        for (Future<JsonNode> future : pool.invokeAll(calls)) {
            results.add(future.get());
        }
        pool.shutdown();

        long assigned = results.stream().filter(t -> t.get("status").asText().equals("ASIGNADO")).count();
        assertThat(assigned).as("tickets assigned to a technician with one free slot").isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                select count(*) from tickets t join technicians k on k.id = t.assigned_technician_id
                where k.code = 'TEC-001' and t.status in ('ASIGNADO', 'EN_PROGRESO')""", Integer.class)).isEqualTo(1);
    }
}
