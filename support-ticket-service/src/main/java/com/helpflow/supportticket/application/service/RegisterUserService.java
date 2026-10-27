package com.helpflow.supportticket.application.service;

import com.helpflow.supportticket.application.port.in.RegisterUserUseCase;
import com.helpflow.supportticket.application.port.out.PasswordHasherPort;
import com.helpflow.supportticket.application.port.out.UserRepositoryPort;
import com.helpflow.supportticket.domain.exception.BusinessRuleViolationException;
import com.helpflow.supportticket.domain.exception.DuplicateResourceException;
import com.helpflow.supportticket.domain.model.CorporateEmail;
import com.helpflow.supportticket.domain.model.PersonalDocument;
import com.helpflow.supportticket.domain.model.Role;
import com.helpflow.supportticket.domain.model.User;
import com.helpflow.supportticket.domain.model.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class RegisterUserService implements RegisterUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterUserService.class);

    private final UserRepositoryPort users;
    private final PasswordHasherPort passwordHasher;

    RegisterUserService(UserRepositoryPort users, PasswordHasherPort passwordHasher) {
        this.users = users;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public User register(Command command) {
        User created = users.save(newUser(command, Role.ROLE_USUARIO, users, passwordHasher));
        log.info("User {} registered with role {}", created.id(), created.role());
        return created;
    }

    /**
     * Shared by self-registration and technician creation: applies the corporate e-mail rule and the
     * uniqueness of document and e-mail before building the user.
     */
    static User newUser(Command command, Role role, UserRepositoryPort users, PasswordHasherPort hasher) {
        String email = CorporateEmail.normalize(command.email());
        if (!CorporateEmail.isValid(email)) {
            throw new BusinessRuleViolationException("invalid-corporate-email",
                    "E-mail " + email + " is not a valid corporate address", email);
        }
        if (users.existsByEmail(email)) {
            throw new DuplicateResourceException("duplicate-email", "E-mail already registered: " + email, email);
        }
        String document = PersonalDocument.normalize(command.document());
        if (users.existsByDocument(document)) {
            throw new DuplicateResourceException("duplicate-document", "Document already registered: " + document,
                    document);
        }
        return new User(null, document, command.fullName().trim(), email, hasher.hash(command.rawPassword()),
                command.site().trim(), command.area().trim(), UserStatus.ACTIVO, role);
    }
}
