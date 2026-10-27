package com.helpflow.supportticket.application.service;

import com.helpflow.supportticket.application.port.in.AuthenticateUserUseCase;
import com.helpflow.supportticket.application.port.out.PasswordHasherPort;
import com.helpflow.supportticket.application.port.out.TicketMetricsPort;
import com.helpflow.supportticket.application.port.out.TokenIssuerPort;
import com.helpflow.supportticket.application.port.out.UserRepositoryPort;
import com.helpflow.supportticket.domain.exception.AccountInactiveException;
import com.helpflow.supportticket.domain.exception.InvalidCredentialsException;
import com.helpflow.supportticket.domain.model.CorporateEmail;
import com.helpflow.supportticket.domain.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class AuthenticateUserService implements AuthenticateUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuthenticateUserService.class);

    private final UserRepositoryPort users;
    private final PasswordHasherPort passwordHasher;
    private final TokenIssuerPort tokenIssuer;
    private final TicketMetricsPort metrics;

    AuthenticateUserService(UserRepositoryPort users, PasswordHasherPort passwordHasher, TokenIssuerPort tokenIssuer,
            TicketMetricsPort metrics) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.metrics = metrics;
    }

    @Override
    public Result login(Command command) {
        User user = users.findByEmail(CorporateEmail.normalize(command.email())).orElse(null);
        if (user == null || !passwordHasher.matches(command.rawPassword(), user.passwordHash())) {
            metrics.authenticationFailed("bad_credentials");
            log.warn("Login rejected: bad credentials");
            throw new InvalidCredentialsException("bad-credentials", "Unknown e-mail or wrong password");
        }
        if (!user.isActive()) {
            metrics.authenticationFailed("inactive_account");
            log.warn("Login rejected: user {} is inactive", user.id());
            throw new AccountInactiveException("account-inactive", "User " + user.id() + " is INACTIVO");
        }
        log.info("User {} authenticated", user.id());
        return new Result(tokenIssuer.issueFor(user), user);
    }
}
