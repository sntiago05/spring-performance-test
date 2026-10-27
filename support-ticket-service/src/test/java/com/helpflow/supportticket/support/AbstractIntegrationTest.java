package com.helpflow.supportticket.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.helpflow.supportticket.application.port.out.TechnicianDispatchPort;
import com.helpflow.supportticket.domain.exception.TechnicianDispatchUnavailableException;
import com.helpflow.supportticket.domain.model.Availability;
import com.helpflow.supportticket.domain.model.DispatchSuggestion;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Full application on a real PostgreSQL (Testcontainers) driven through MockMvc. Credentials are random
 * per run; technician-dispatch-service is replaced by a mock of its outbound port.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfig.class, AbstractIntegrationTest.ClockConfig.class})
public abstract class AbstractIntegrationTest {

    protected static final String ADMIN_EMAIL = "admin@helpflow.example";
    protected static final String PASSWORD = "Pw-" + UUID.randomUUID();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt.secret", () -> "test-secret-" + UUID.randomUUID() + UUID.randomUUID());
        registry.add("app.bootstrap.admin-email", () -> ADMIN_EMAIL);
        registry.add("app.bootstrap.admin-password", () -> PASSWORD);
        registry.add("app.bootstrap.technician-password", () -> PASSWORD);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock();
        }
    }

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected MutableClock clock;
    @MockitoBean
    protected TechnicianDispatchPort dispatchPort;

    @Autowired
    protected JdbcTemplate jdbc;

    @BeforeEach
    void resetState() {
        clock.reset();
        jdbc.update("delete from ticket_assignments");
        jdbc.update("delete from ticket_history");
        jdbc.update("delete from tickets");
        // Default: the remote service is down, so tests opt in to a suggestion with suggest(...)
        when(dispatchPort.suggest(any(), any(), any())).thenThrow(
                new TechnicianDispatchUnavailableException("dispatch-unavailable", "stubbed as down"));
    }

    @AfterEach
    void restoreSeedTechnicians() {
        jdbc.update("""
                update technicians set status = 'ACTIVO', max_active_tickets = case specialty
                  when 'HARDWARE' then 3 when 'SOFTWARE' then 4 when 'RED' then 3 else 5 end
                where code <= 'TEC-015'""");
        jdbc.update("update sla_policies set resolution_hours = case priority when 'CRITICA' then 4 "
                + "when 'ALTA' then 8 when 'MEDIA' then 24 else 72 end, escalation_threshold_percent = 80");
    }

    /** The dispatch service will suggest this technician with this estimate. */
    protected void suggest(String technicianCode, int estimatedHours) {
        doReturn(new DispatchSuggestion(technicianCode,
                estimatedHours <= 2 ? Availability.ALTA : estimatedHours <= 8 ? Availability.MEDIA : Availability.BAJA,
                estimatedHours)).when(dispatchPort).suggest(any(), any(), any());
    }

    protected String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    // ---------- HTTP helpers ----------

    protected JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder builder, String token) {
        return token == null ? builder : builder.header("Authorization", "Bearer " + token);
    }

    protected MockHttpServletRequestBuilder jsonBody(MockHttpServletRequestBuilder builder, Object body)
            throws Exception {
        return builder.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
    }

    protected MvcResult postJson(String path, String token, Object body) throws Exception {
        return mockMvc.perform(authed(jsonBody(post(path), body), token)).andReturn();
    }

    protected MvcResult putJson(String path, String token, Object body) throws Exception {
        return mockMvc.perform(authed(jsonBody(put(path), body), token)).andReturn();
    }

    protected MvcResult patchJson(String path, String token, Object body) throws Exception {
        return mockMvc.perform(authed(jsonBody(patch(path), body), token)).andReturn();
    }

    protected MvcResult getJson(String path, String token) throws Exception {
        return mockMvc.perform(authed(get(path), token)).andReturn();
    }

    // ---------- Scenario helpers ----------

    protected String login(String email, String password) throws Exception {
        MvcResult result = postJson("/auth/login", null, Map.of("email", email, "password", password));
        return json(result).get("accessToken").asString();
    }

    protected String adminToken() throws Exception {
        return login(ADMIN_EMAIL, PASSWORD);
    }

    /** Technician accounts are bootstrapped as tec-001@..., one per seeded technician code. */
    protected String technicianToken(String code) throws Exception {
        return login(code.toLowerCase() + "@helpflow.example", PASSWORD);
    }

    /** Registers a fresh user and returns the JWT. */
    protected String newUserToken() throws Exception {
        String suffix = uniqueSuffix();
        String email = "user." + suffix + "@helpflow.example";
        postJson("/auth/register", null, Map.of("document", "DOC-" + suffix, "fullName", "User " + suffix,
                "email", email, "password", PASSWORD, "site", "BOGOTA", "area", "Finance"));
        return login(email, PASSWORD);
    }

    protected Map<String, Object> ticketBody(String category, String priority, String site) {
        return Map.of("title", "Problem with " + category, "description", "Detailed description of the problem",
                "category", category, "priority", priority, "site", site, "affectedAsset", "ASSET-001");
    }

    protected JsonNode createTicket(String token, String category, String priority, String site) throws Exception {
        MvcResult result = postJson("/tickets", token, ticketBody(category, priority, site));
        org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus())
                .as(result.getResponse().getContentAsString()).isEqualTo(201);
        return json(result);
    }

    protected Clock clock() {
        return clock;
    }
}
