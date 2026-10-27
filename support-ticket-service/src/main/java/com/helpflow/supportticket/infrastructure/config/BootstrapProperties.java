package com.helpflow.supportticket.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Optional first-run provisioning of login accounts. Nothing is created unless a password is given
 * through the environment, so the repository carries no credentials.
 */
@ConfigurationProperties(prefix = "app.bootstrap")
public record BootstrapProperties(String adminEmail, String adminPassword, String technicianPassword,
        String technicianEmailDomain) {

    public BootstrapProperties {
        adminEmail = isBlank(adminEmail) ? "admin@helpflow.example" : adminEmail;
        technicianEmailDomain = isBlank(technicianEmailDomain) ? "helpflow.example" : technicianEmailDomain;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
