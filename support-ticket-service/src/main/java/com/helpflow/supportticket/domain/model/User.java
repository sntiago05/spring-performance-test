package com.helpflow.supportticket.domain.model;

public record User(
        Long id,
        String document,
        String fullName,
        String email,
        String passwordHash,
        String site,
        String area,
        UserStatus status,
        Role role) {

    public boolean isActive() {
        return status == UserStatus.ACTIVO;
    }

    public User withProfile(String fullName, String site, String area) {
        return new User(id, document, fullName, email, passwordHash, site, area, status, role);
    }

    public User withStatus(UserStatus newStatus) {
        return new User(id, document, fullName, email, passwordHash, site, area, newStatus, role);
    }
}
