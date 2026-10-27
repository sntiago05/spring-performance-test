package com.helpflow.supportticket.infrastructure.adapter.in.web;

import com.helpflow.supportticket.domain.model.Actor;
import com.helpflow.supportticket.domain.model.Role;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Turns the validated JWT of the current request into the domain {@link Actor}.
 */
@Component
public class AuthenticatedActor {

    public Actor current() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Number userId = jwt.getClaim("uid");
        var roles = jwt.getClaimAsStringList("roles");
        return new Actor(userId.longValue(), jwt.getSubject(), Role.valueOf(roles.get(0)));
    }
}
