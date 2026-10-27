package com.helpflow.supportticket.application.port.in;

import com.helpflow.supportticket.domain.model.User;

public interface RegisterUserUseCase {

    /**
     * Self-registration: always creates an ACTIVO user with the ROLE_USUARIO role.
     */
    User register(Command command);

    record Command(String document, String fullName, String email, String rawPassword, String site, String area) {
    }
}
