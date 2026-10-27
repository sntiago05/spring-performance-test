package com.helpflow.supportticket.domain.model;

/**
 * The authenticated subject performing an operation.
 */
public record Actor(Long userId, String email, Role role) {

    public boolean isAdmin() {
        return role == Role.ROLE_ADMIN;
    }

    public boolean isTechnician() {
        return role == Role.ROLE_TECNICO;
    }

    public boolean isRegularUser() {
        return role == Role.ROLE_USUARIO;
    }
}
