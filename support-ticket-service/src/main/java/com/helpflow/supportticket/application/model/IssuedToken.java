package com.helpflow.supportticket.application.model;

import java.time.Instant;

public record IssuedToken(String value, Instant expiresAt) {
}
