package com.helpflow.supportticket.infrastructure.adapter.out.security;

import com.helpflow.supportticket.application.model.IssuedToken;
import com.helpflow.supportticket.application.port.out.TokenIssuerPort;
import com.helpflow.supportticket.domain.model.User;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Issues signed, stateless access tokens (HS256). Claims: {@code sub} = e-mail, {@code uid} = user id,
 * {@code roles} = granted role names.
 */
@Component
class JwtTokenIssuerAdapter implements TokenIssuerPort {

    static final String USER_ID_CLAIM = "uid";
    static final String ROLES_CLAIM = "roles";

    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    JwtTokenIssuerAdapter(JwtEncoder encoder, JwtProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public IssuedToken issueFor(User user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.expiration());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.email())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim(USER_ID_CLAIM, user.id())
                .claim(ROLES_CLAIM, List.of(user.role().name()))
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }
}
