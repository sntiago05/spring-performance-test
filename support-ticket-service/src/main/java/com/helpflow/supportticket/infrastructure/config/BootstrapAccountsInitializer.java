package com.helpflow.supportticket.infrastructure.config;

import com.helpflow.supportticket.application.port.out.PasswordHasherPort;
import com.helpflow.supportticket.application.port.out.TechnicianRepositoryPort;
import com.helpflow.supportticket.application.port.out.UserRepositoryPort;
import com.helpflow.supportticket.domain.model.Role;
import com.helpflow.supportticket.domain.model.User;
import com.helpflow.supportticket.domain.model.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Creates, only when passwords are provided in the environment, the admin account and one login
 * account per seeded technician (tec-001@..., tec-002@...). Idempotent: existing accounts are kept.
 */
@Component
@EnableConfigurationProperties(BootstrapProperties.class)
class BootstrapAccountsInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAccountsInitializer.class);

    private final BootstrapProperties properties;
    private final UserRepositoryPort users;
    private final TechnicianRepositoryPort technicians;
    private final PasswordHasherPort passwordHasher;

    BootstrapAccountsInitializer(BootstrapProperties properties, UserRepositoryPort users,
            TechnicianRepositoryPort technicians, PasswordHasherPort passwordHasher) {
        this.properties = properties;
        this.users = users;
        this.technicians = technicians;
        this.passwordHasher = passwordHasher;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (StringUtils.hasText(properties.adminPassword()) && !users.existsByEmail(properties.adminEmail())) {
            users.save(new User(null, "ADMIN-0001", "HelpFlow Administrator", properties.adminEmail(),
                    passwordHasher.hash(properties.adminPassword()), "BOGOTA", "IT", UserStatus.ACTIVO,
                    Role.ROLE_ADMIN));
            log.info("Bootstrap admin account created: {}", properties.adminEmail());
        }
        if (StringUtils.hasText(properties.technicianPassword())) {
            technicians.findWithoutUser().forEach(technician -> {
                String code = technician.code().toLowerCase();
                User account = users.save(new User(null, "DOC-" + technician.code(), technician.fullName(),
                        code + "@" + properties.technicianEmailDomain(),
                        passwordHasher.hash(properties.technicianPassword()), technician.site(), "IT Support",
                        UserStatus.ACTIVO, Role.ROLE_TECNICO));
                technicians.save(technician.withUser(account.id()));
                log.info("Bootstrap technician account created: {}", account.email());
            });
        }
    }
}
