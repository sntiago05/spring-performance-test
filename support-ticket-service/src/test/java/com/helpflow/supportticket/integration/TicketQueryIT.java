package com.helpflow.supportticket.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.helpflow.supportticket.support.AbstractIntegrationTest;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Listing, filtering, sorting, pagination and the N+1 guarantee.
 */
class TicketQueryIT extends AbstractIntegrationTest {

    @Autowired
    EntityManagerFactory entityManagerFactory;

    private String admin;
    private String user;

    @BeforeEach
    void tickets() throws Exception {
        admin = adminToken();
        user = newUserToken();
        String other = newUserToken();
        for (String priority : List.of("BAJA", "MEDIA", "ALTA")) {
            createTicket(user, "RED", priority, "BOGOTA");
            createTicket(user, "SOFTWARE", priority, "CALI");
        }
        createTicket(other, "OTRO", "CRITICA", "MEDELLIN");
    }

    @Test
    void paginationReportsTheTotalsAndSortingIsStable() throws Exception {
        JsonNode firstPage = json(getJson("/tickets?size=3&page=0&sort=createdAt,asc", admin));
        JsonNode lastPage = json(getJson("/tickets?size=3&page=2&sort=createdAt,asc", admin));

        assertThat(firstPage.get("totalElements").asInt()).isEqualTo(7);
        assertThat(firstPage.get("totalPages").asInt()).isEqualTo(3);
        assertThat(firstPage.get("content")).hasSize(3);
        assertThat(lastPage.get("content")).hasSize(1);
        List<Long> ids = new java.util.ArrayList<>();
        firstPage.get("content").forEach(ticket -> ids.add(ticket.get("id").asLong()));
        assertThat(ids).isSorted();
    }

    @Test
    void filtersCombine() throws Exception {
        JsonNode red = json(getJson("/tickets?category=RED", admin));
        JsonNode highSoftware = json(getJson("/tickets?category=software&priority=ALTA", admin));
        JsonNode critical = json(getJson("/tickets?priority=CRÍTICA", admin));
        JsonNode assigned = json(getJson("/tickets?status=ASIGNADO", admin));
        JsonNode open = json(getJson("/tickets?status=ABIERTO", admin));

        assertThat(red.get("totalElements").asInt()).isEqualTo(3);
        assertThat(highSoftware.get("totalElements").asInt()).isEqualTo(1);
        assertThat(critical.get("totalElements").asInt()).isEqualTo(1);
        assertThat(assigned.get("totalElements").asInt()).isEqualTo(7);
        assertThat(open.get("totalElements").asInt()).isZero();
    }

    @Test
    void adminsCanFilterByRequesterAndTechnician() throws Exception {
        long userId = json(getJson("/users/me", user)).get("id").asLong();

        JsonNode byRequester = json(getJson("/tickets?requesterId=" + userId, admin));
        JsonNode byTechnician = json(getJson("/tickets?technicianId=" + jdbc.queryForObject(
                "select id from technicians where code = 'TEC-007'", Long.class), admin));

        assertThat(byRequester.get("totalElements").asInt()).isEqualTo(6);
        assertThat(byTechnician.get("content").findValues("id")).isNotEmpty();
        assertThat(byTechnician.get("content").findValues("code")).containsOnly(
                byTechnician.get("content").get(0).get("technician").get("code"));
    }

    @Test
    void invalidPagingAndSortParametersAreBadRequests() throws Exception {
        for (String query : List.of("sort=password,asc", "sort=createdAt,sideways", "size=0", "size=1000", "page=-1",
                "status=NOPE")) {
            MvcResult result = getJson("/tickets?" + query, admin);
            assertThat(result.getResponse().getStatus()).as(query).isEqualTo(400);
            assertThat(result.getResponse().getContentType()).startsWith("application/problem+json");
        }
    }

    @Test
    void listingATenElementPageDoesNotTriggerNPlusOneQueries() throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        MvcResult result = getJson("/tickets?size=10", admin);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(result).get("content")).hasSize(7);
        // one select (tickets + requester + technician joined) and one count, whatever the page size
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
    }

    @Test
    void singleTicketAndTechnicianScopedListingsStayConstantInQueries() throws Exception {
        long id = json(getJson("/tickets?size=1", admin)).get("content").get(0).get("id").asLong();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();

        statistics.clear();
        getJson("/tickets/" + id, admin);
        long single = statistics.getPrepareStatementCount();
        statistics.clear();
        getJson("/tickets?size=10", technicianToken("TEC-007"));
        long technicianListing = statistics.getPrepareStatementCount();

        assertThat(single).isEqualTo(1); // ticket + requester + technician in a single join
        assertThat(technicianListing).isLessThanOrEqualTo(3); // technician profile + page + count
    }

    @Test
    void technicianManagementCreatesAnAccountAndAProfileTogether() throws Exception {
        String suffix = uniqueSuffix();
        Map<String, Object> body = Map.of("document", "TECDOC-" + suffix, "fullName", "Carlos Ruiz",
                "email", "carlos." + suffix + "@helpflow.example", "password", PASSWORD, "site", "BOGOTA",
                "area", "IT Support", "specialty", "RED", "maxActiveTickets", 2);

        MvcResult created = postJson("/technicians", admin, body);
        MvcResult duplicate = postJson("/technicians", admin, body);

        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        JsonNode technician = json(created);
        assertThat(technician.get("code").asText()).matches("TEC-0(1[6-9]|[2-9]\\d)");
        assertThat(technician.get("userId").isNull()).isFalse();
        assertThat(duplicate.getResponse().getStatus()).isEqualTo(409);
        // the new technician can log in with ROLE_TECNICO
        String token = login("carlos." + suffix + "@helpflow.example", PASSWORD);
        assertThat(json(getJson("/users/me", token)).get("role").asText()).isEqualTo("ROLE_TECNICO");

        MvcResult updated = putJson("/technicians/" + technician.get("id").asLong(), admin, Map.of("fullName",
                "Carlos Ruiz Mora", "specialty", "SOFTWARE", "site", "CALI", "maxActiveTickets", 4, "status", "INACTIVO"));
        assertThat(json(updated).get("specialty").asText()).isEqualTo("SOFTWARE");
        assertThat(json(updated).get("status").asText()).isEqualTo("INACTIVO");
        assertThat(json(getJson("/technicians?status=INACTIVO&specialty=SOFTWARE", admin)).get("totalElements").asInt())
                .isEqualTo(1);
    }
}
