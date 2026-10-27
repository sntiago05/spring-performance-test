package com.helpflow.supportticket.integration;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.JsonNode;
import com.helpflow.supportticket.support.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class AuthenticationIT extends AbstractIntegrationTest {

    private Map<String, Object> registration(String suffix, String email) {
        return Map.of("document", "DOC-" + suffix, "fullName", "Laura " + suffix, "email", email,
                "password", PASSWORD, "site", "BOGOTA", "area", "Finance");
    }

    @Test
    void registrationCreatesAnActiveUserWithoutExposingThePassword() throws Exception {
        String suffix = uniqueSuffix();

        MvcResult result = postJson("/auth/register", null, registration(suffix, "laura." + suffix + "@helpflow.example"));

        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        JsonNode body = json(result);
        assertThat(body.get("role").asString()).isEqualTo("ROLE_USUARIO");
        assertThat(body.get("status").asString()).isEqualTo("ACTIVO");
        assertThat(body.has("password")).isFalse();
        assertThat(body.has("passwordHash")).isFalse();
        assertThat(jdbc.queryForObject("select password_hash from users where email = ?", String.class,
                "laura." + suffix + "@helpflow.example")).startsWith("$2a$").doesNotContain(PASSWORD);
    }

    @Test
    void duplicatedEmailAndDocumentAreConflicts() throws Exception {
        String suffix = uniqueSuffix();
        String email = "dup." + suffix + "@helpflow.example";
        postJson("/auth/register", null, registration(suffix, email));

        MvcResult sameEmail = postJson("/auth/register", null, registration(uniqueSuffix(), email));
        MvcResult sameDocument = postJson("/auth/register", null,
                registration(suffix, "other." + suffix + "@helpflow.example"));

        assertThat(sameEmail.getResponse().getStatus()).isEqualTo(409);
        assertThat(json(sameEmail).get("type").asString()).isEqualTo("urn:helpflow:problem:duplicate-email");
        assertThat(sameDocument.getResponse().getStatus()).isEqualTo(409);
        assertThat(json(sameDocument).get("type").asString()).isEqualTo("urn:helpflow:problem:duplicate-document");
    }

    @Test
    void publicMailProvidersAndWeakPasswordsAreRejectedWithFieldErrors() throws Exception {
        String suffix = uniqueSuffix();
        Map<String, Object> body = Map.of("document", "DOC-" + suffix, "fullName", "Laura", "email",
                "laura@gmail.com", "password", "short", "site", "BOGOTA", "area", "Finance");

        MvcResult result = postJson("/auth/register", null, body);

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        JsonNode problem = json(result);
        assertThat(problem.get("title").asString()).isEqualTo("Validation failed");
        assertThat(problem.get("errors").findValuesAsString("field")).contains("email", "password");
    }

    @Test
    void loginReturnsABearerTokenForTheRegisteredUser() throws Exception {
        String suffix = uniqueSuffix();
        String email = "login." + suffix + "@helpflow.example";
        postJson("/auth/register", null, registration(suffix, email));

        MvcResult result = postJson("/auth/login", null, Map.of("email", email, "password", PASSWORD));

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode body = json(result);
        assertThat(body.get("tokenType").asString()).isEqualTo("Bearer");
        assertThat(body.get("accessToken").asString().split("\\.")).hasSize(3);
        assertThat(body.get("user").get("email").asString()).isEqualTo(email);

        MvcResult me = getJson("/users/me", body.get("accessToken").asString());
        assertThat(me.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(me).get("email").asString()).isEqualTo(email);
    }

    @Test
    void wrongPasswordAndUnknownUserGetTheSameAnswer() throws Exception {
        String suffix = uniqueSuffix();
        String email = "bad." + suffix + "@helpflow.example";
        postJson("/auth/register", null, registration(suffix, email));

        MvcResult wrongPassword = postJson("/auth/login", null, Map.of("email", email, "password", "not-the-password"));
        MvcResult unknown = postJson("/auth/login", null, Map.of("email", "nobody@helpflow.example", "password", PASSWORD));

        assertThat(wrongPassword.getResponse().getStatus()).isEqualTo(401);
        assertThat(unknown.getResponse().getStatus()).isEqualTo(401);
        assertThat(json(wrongPassword).get("detail")).isEqualTo(json(unknown).get("detail"));
    }

    @Test
    void anInactiveAccountCannotLogInAndAnAdminCanReactivateIt() throws Exception {
        String suffix = uniqueSuffix();
        String email = "inactive." + suffix + "@helpflow.example";
        long id = json(postJson("/auth/register", null, registration(suffix, email))).get("id").asLong();
        String admin = adminToken();

        patchJson("/users/" + id + "/status", admin, Map.of("status", "INACTIVO"));
        MvcResult blocked = postJson("/auth/login", null, Map.of("email", email, "password", PASSWORD));
        patchJson("/users/" + id + "/status", admin, Map.of("status", "ACTIVO"));
        MvcResult restored = postJson("/auth/login", null, Map.of("email", email, "password", PASSWORD));

        assertThat(blocked.getResponse().getStatus()).isEqualTo(403);
        assertThat(json(blocked).get("type").asString()).isEqualTo("urn:helpflow:problem:account-inactive");
        assertThat(restored.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void protectedEndpointsRejectMissingAndForgedTokensWithAProblemDocument() throws Exception {
        MvcResult noToken = getJson("/tickets", null);
        MvcResult garbage = getJson("/tickets", "not.a.jwt");
        String forged = adminToken().substring(0, adminToken().lastIndexOf('.') + 1) + "AAAA";
        MvcResult tampered = getJson("/tickets", forged);

        for (MvcResult result : new MvcResult[] {noToken, garbage, tampered}) {
            assertThat(result.getResponse().getStatus()).isEqualTo(401);
            assertThat(result.getResponse().getContentType()).startsWith("application/problem+json");
            assertThat(result.getResponse().getHeader("WWW-Authenticate")).isEqualTo("Bearer");
            JsonNode problem = json(result);
            assertThat(problem.get("status").asInt()).isEqualTo(401);
            assertThat(problem.get("traceId").asString()).isNotBlank();
            assertThat(problem.get("instance").asString()).isEqualTo("/tickets");
        }
    }

    @Test
    void usersCanReadAndEditOnlyTheirOwnProfile() throws Exception {
        String suffix = uniqueSuffix();
        String email = "profile." + suffix + "@helpflow.example";
        long myId = json(postJson("/auth/register", null, registration(suffix, email))).get("id").asLong();
        String token = login(email, PASSWORD);
        long otherId = json(getJson("/users/me", newUserToken())).get("id").asLong();

        MvcResult updated = putJson("/users/" + myId, token,
                Map.of("fullName", "Laura Edited", "site", "CALI", "area", "Accounting"));
        MvcResult foreign = getJson("/users/" + otherId, token);
        MvcResult foreignEdit = putJson("/users/" + otherId, token,
                Map.of("fullName", "Hacked", "site", "CALI", "area", "X"));

        assertThat(updated.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(updated).get("fullName").asString()).isEqualTo("Laura Edited");
        assertThat(json(updated).get("email").asString()).isEqualTo(email); // immutable
        assertThat(foreign.getResponse().getStatus()).isEqualTo(403);
        assertThat(foreignEdit.getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void theSameDocumentTypedWithOtherCaseOrSpacesIsStillADuplicate() throws Exception {
        String suffix = uniqueSuffix();
        postJson("/auth/register", null, Map.of("document", "ab-" + suffix, "fullName", "Same Person",
                "email", "first." + suffix + "@helpflow.example", "password", PASSWORD, "site", "BOGOTA", "area", "X"));

        MvcResult again = postJson("/auth/register", null, Map.of("document", "AB-" + suffix.toUpperCase(),
                "fullName", "Same Person", "email", "second." + suffix + "@helpflow.example", "password", PASSWORD,
                "site", "BOGOTA", "area", "X"));

        assertThat(again.getResponse().getStatus()).isEqualTo(409);
        assertThat(json(again).get("type").asText()).endsWith("duplicate-document");
        assertThat(jdbc.queryForObject("select document from users where email = ?", String.class,
                "first." + suffix + "@helpflow.example")).isEqualTo("AB-" + suffix.toUpperCase());
    }

    @Test
    void anAdminCannotDeactivateTheirOwnAccount() throws Exception {
        String admin = adminToken();
        long adminId = json(getJson("/users/me", admin)).get("id").asLong();

        MvcResult result = patchJson("/users/" + adminId + "/status", admin, Map.of("status", "INACTIVO"));

        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        assertThat(json(result).get("type").asText()).endsWith("cannot-deactivate-self");
        assertThat(json(getJson("/users/me", admin)).get("status").asText()).isEqualTo("ACTIVO");
    }

    @Test
    void deactivatingATechnicianAccountStopsTicketsFromBeingAssignedToThem() throws Exception {
        String admin = adminToken();
        long userId = jdbc.queryForObject("select user_id from technicians where code = 'TEC-007'", Long.class);
        String user = newUserToken();
        try {
            patchJson("/users/" + userId + "/status", admin, Map.of("status", "INACTIVO"));

            JsonNode ticket = createTicket(user, "RED", "MEDIA", "BOGOTA"); // TEC-007 would be the first choice

            assertThat(jdbc.queryForObject("select status from technicians where code = 'TEC-007'", String.class))
                    .isEqualTo("INACTIVO");
            assertThat(ticket.get("technician").get("code").asText()).isNotEqualTo("TEC-007");
        } finally {
            patchJson("/users/" + userId + "/status", admin, Map.of("status", "ACTIVO"));
        }
        assertThat(jdbc.queryForObject("select status from technicians where code = 'TEC-007'", String.class))
                .isEqualTo("ACTIVO");
    }
}
