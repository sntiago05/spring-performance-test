package com.helpflow.supportticket.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.helpflow.supportticket.support.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Roles (URL level) and ownership (data level): who can create, see and manage which tickets.
 */
class TicketAuthorizationIT extends AbstractIntegrationTest {

    private String owner;
    private String stranger;
    private String assignedTechnician;
    private String otherTechnician;
    private String admin;
    private long ticketId;

    @BeforeEach
    void scenario() throws Exception {
        suggest("TEC-008", 3);
        owner = newUserToken();
        stranger = newUserToken();
        assignedTechnician = technicianToken("TEC-008");
        otherTechnician = technicianToken("TEC-009");
        admin = adminToken();
        ticketId = createTicket(owner, "RED", "MEDIA", "MEDELLIN").get("id").asLong();
    }

    private int status(MvcResult result) {
        return result.getResponse().getStatus();
    }

    @Test
    void theOwnerSeesTheTicketAndAStrangerDoesNot() throws Exception {
        assertThat(status(getJson("/tickets/" + ticketId, owner))).isEqualTo(200);

        MvcResult denied = getJson("/tickets/" + ticketId, stranger);

        assertThat(status(denied)).isEqualTo(403);
        assertThat(json(denied).get("type").asText()).isEqualTo("urn:helpflow:problem:ticket-access-denied");
        assertThat(status(getJson("/tickets/" + ticketId + "/history", stranger))).isEqualTo(403);
        assertThat(status(getJson("/tickets/" + ticketId + "/assignments", stranger))).isEqualTo(403);
    }

    @Test
    void listingIsAlwaysScopedToTheCaller() throws Exception {
        suggest("TEC-012", 3); // the stranger's ticket goes to another technician
        createTicket(stranger, "OTRO", "BAJA", "CALI");
        long strangerId = json(getJson("/users/me", stranger)).get("id").asLong();

        // the stranger even asks for someone else's tickets explicitly
        JsonNode strangerList = json(getJson("/tickets?requesterId=" + json(getJson("/users/me", owner)).get("id").asLong(),
                stranger));
        JsonNode technicianList = json(getJson("/tickets", assignedTechnician));
        JsonNode adminList = json(getJson("/tickets", admin));

        assertThat(strangerList.get("totalElements").asInt()).isEqualTo(1);
        assertThat(strangerList.get("content").get(0).get("requester").get("id").asLong()).isEqualTo(strangerId);
        assertThat(technicianList.get("totalElements").asInt()).isEqualTo(1);
        assertThat(technicianList.get("content").get(0).get("id").asLong()).isEqualTo(ticketId);
        assertThat(adminList.get("totalElements").asInt()).isEqualTo(2);
    }

    @Test
    void theAssignedTechnicianManagesTheTicketButAnotherTechnicianCannotEvenSeeIt() throws Exception {
        assertThat(status(getJson("/tickets/" + ticketId, assignedTechnician))).isEqualTo(200);

        MvcResult read = getJson("/tickets/" + ticketId, otherTechnician);
        MvcResult write = patchJson("/tickets/" + ticketId + "/status", otherTechnician,
                Map.of("status", "EN_PROGRESO", "observations", "Not mine"));

        assertThat(status(read)).isEqualTo(403);
        assertThat(status(write)).isEqualTo(403);
        assertThat(json(write).get("type").asText()).isEqualTo("urn:helpflow:problem:ticket-manage-denied");
        assertThat(json(getJson("/tickets", otherTechnician)).get("totalElements").asInt()).isZero();
    }

    @Test
    void usersCannotManageTickets() throws Exception {
        MvcResult changeStatus = patchJson("/tickets/" + ticketId + "/status", owner,
                Map.of("status", "CANCELADO", "observations", "I want to cancel my ticket please"));
        MvcResult reassign = putJson("/tickets/" + ticketId + "/assignment", owner,
                Map.of("technicianId", 9, "observations", "Give it to somebody"));

        assertThat(status(changeStatus)).isEqualTo(403);
        assertThat(status(reassign)).isEqualTo(403);
    }

    @Test
    void techniciansCannotCreateTicketsReassignOrCancel() throws Exception {
        MvcResult create = postJson("/tickets", assignedTechnician, ticketBody("RED", "BAJA", "CALI"));
        MvcResult reassign = putJson("/tickets/" + ticketId + "/assignment", assignedTechnician,
                Map.of("technicianId", 9, "observations", "Give it away"));
        MvcResult cancel = patchJson("/tickets/" + ticketId + "/status", assignedTechnician,
                Map.of("status", "CANCELADO", "observations", "Cancelled because it is a duplicate"));

        assertThat(status(create)).isEqualTo(403);
        assertThat(status(reassign)).isEqualTo(403);
        assertThat(status(cancel)).isEqualTo(403);
        assertThat(json(cancel).get("type").asText()).isEqualTo("urn:helpflow:problem:cancel-admin-only");
    }

    @Test
    void adminsHaveFullAccess() throws Exception {
        assertThat(status(getJson("/tickets/" + ticketId, admin))).isEqualTo(200);
        assertThat(status(getJson("/tickets/" + ticketId + "/history", admin))).isEqualTo(200);
        assertThat(status(patchJson("/tickets/" + ticketId + "/status", admin,
                Map.of("status", "CANCELADO", "observations", "Cancelled by the administrator")))).isEqualTo(200);
    }

    @Test
    void adminOnlyEndpointsRejectEveryOtherRole() throws Exception {
        for (String token : new String[] {owner, assignedTechnician}) {
            assertThat(status(getJson("/users", token))).isEqualTo(403);
            assertThat(status(getJson("/technicians", token))).isEqualTo(403);
            assertThat(status(putJson("/sla-policies/ALTA", token,
                    Map.of("resolutionHours", 6, "escalationThresholdPercent", 70)))).isEqualTo(403);
            assertThat(status(postJson("/tickets/escalations", token, Map.of()))).isEqualTo(403);
            assertThat(status(patchJson("/users/1/status", token, Map.of("status", "INACTIVO")))).isEqualTo(403);
        }
        assertThat(status(getJson("/users", admin))).isEqualTo(200);
        assertThat(status(getJson("/technicians", admin))).isEqualTo(200);
    }

    @Test
    void anyAuthenticatedRoleCanReadTheSlaPolicies() throws Exception {
        for (String token : new String[] {owner, assignedTechnician, admin}) {
            MvcResult result = getJson("/sla-policies", token);
            assertThat(status(result)).isEqualTo(200);
            assertThat(json(result).findValuesAsText("priority")).containsExactly("BAJA", "MEDIA", "ALTA", "CRITICA");
        }
    }

    @Test
    void forbiddenAndUnauthorizedAnswersAreCountedInTheMetrics() throws Exception {
        getJson("/tickets/" + ticketId, stranger); // 403
        getJson("/tickets", null); // 401
        postJson("/auth/login", null, Map.of("email", "nobody@helpflow.example", "password", "wrong-password"));

        String metrics = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/actuator/prometheus")).andReturn().getResponse().getContentAsString();

        assertThat(metrics).containsPattern("helpflow_access_denied_total\\{[^}]*\\} [1-9]");
        assertThat(metrics).containsPattern("helpflow_auth_failures_total\\{[^}]*reason=\"unauthenticated\"[^}]*\\} [1-9]");
        assertThat(metrics).containsPattern("helpflow_auth_failures_total\\{[^}]*reason=\"bad_credentials\"[^}]*\\} [1-9]");
    }
}
