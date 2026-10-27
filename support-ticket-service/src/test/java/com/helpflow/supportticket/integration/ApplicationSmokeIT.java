package com.helpflow.supportticket.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.helpflow.supportticket.support.AbstractIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The context starts, Flyway applied V1-V3 and Hibernate validated the mapping against the schema.
 */
class ApplicationSmokeIT extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void flywayAppliesAllMigrationsInOrder() {
        List<String> versions = jdbc.queryForList(
                "select version || ':' || description from flyway_schema_history where success order by installed_rank",
                String.class);

        assertThat(versions).containsExactly("1:schema", "2:asignaciones historial sla", "3:datos iniciales");
    }

    @Test
    void seedDataIsPresent() {
        assertThat(jdbc.queryForObject("select count(*) from technicians where code <= 'TEC-015'", Integer.class)).isEqualTo(15);
        assertThat(jdbc.queryForObject("select count(*) from sla_policies", Integer.class)).isEqualTo(4);
    }

    @Test
    void bootstrapCreatedTheAdminAndOneAccountPerTechnician() {
        assertThat(jdbc.queryForObject("select count(*) from users where role = 'ROLE_ADMIN'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from technicians where user_id is not null and code <= 'TEC-015'", Integer.class))
                .isEqualTo(15);
    }
}
