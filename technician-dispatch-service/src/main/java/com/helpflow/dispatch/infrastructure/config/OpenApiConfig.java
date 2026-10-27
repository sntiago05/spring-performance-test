package com.helpflow.dispatch.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI dispatchOpenApi() {
        return new OpenAPI().info(new Info()
                .title("technician-dispatch-service")
                .version("1.0.0")
                .description("""
                        Simulated external service that suggests an available technician and an estimated \
                        attention time for a support ticket.

                        The answer is **deterministic**: the same `ticketId` + `categoria` + `prioridad` always \
                        returns the same technician and estimate. It is stateless, uses no database and no \
                        authentication; it is meant to be reachable only from `support-ticket-service`.

                        Error messages are localized: send `Accept-Language: es` for Spanish (default: English).
                        """));
    }
}
