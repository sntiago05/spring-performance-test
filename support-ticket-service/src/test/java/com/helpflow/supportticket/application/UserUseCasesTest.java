package com.helpflow.supportticket.application.service;

import static com.helpflow.supportticket.support.Fixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.helpflow.supportticket.application.model.IssuedToken;
import com.helpflow.supportticket.application.port.in.AuthenticateUserUseCase;
import com.helpflow.supportticket.application.port.in.RegisterUserUseCase;
import com.helpflow.supportticket.application.port.out.PasswordHasherPort;
import com.helpflow.supportticket.application.port.out.TicketMetricsPort;
import com.helpflow.supportticket.application.port.out.TokenIssuerPort;
import com.helpflow.supportticket.application.port.out.UserRepositoryPort;
import com.helpflow.supportticket.domain.exception.AccountInactiveException;
import com.helpflow.supportticket.domain.exception.BusinessRuleViolationException;
import com.helpflow.supportticket.domain.exception.DuplicateResourceException;
import com.helpflow.supportticket.domain.exception.InvalidCredentialsException;
import com.helpflow.supportticket.domain.model.Role;
import com.helpflow.supportticket.domain.model.User;
import com.helpflow.supportticket.domain.model.UserStatus;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserUseCasesTest {

    @Mock UserRepositoryPort users;
    @Mock PasswordHasherPort hasher;
    @Mock TokenIssuerPort tokens;
    @Mock TicketMetricsPort metrics;

    @Nested
    class Registration {

        private final RegisterUserUseCase.Command command = new RegisterUserUseCase.Command("1020304050",
                " Laura Gomez ", "Laura.Gomez@HelpFlow.Example", "Str0ngPass!", "BOGOTA", "Finance");

        @Test
        void createsAnActiveUserWithTheUserRoleAndAHashedPassword() {
            when(hasher.hash("Str0ngPass!")).thenReturn("$2a$hash");
            when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            new RegisterUserService(users, hasher).register(command);

            ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
            verify(users).save(saved.capture());
            assertThat(saved.getValue().role()).isEqualTo(Role.ROLE_USUARIO);
            assertThat(saved.getValue().status()).isEqualTo(UserStatus.ACTIVO);
            assertThat(saved.getValue().email()).isEqualTo("laura.gomez@helpflow.example");
            assertThat(saved.getValue().fullName()).isEqualTo("Laura Gomez");
            assertThat(saved.getValue().passwordHash()).isEqualTo("$2a$hash").isNotEqualTo("Str0ngPass!");
        }

        @Test
        void rejectsDuplicatedEmail() {
            when(users.existsByEmail("laura.gomez@helpflow.example")).thenReturn(true);

            assertThatThrownBy(() -> new RegisterUserService(users, hasher).register(command))
                    .isInstanceOf(DuplicateResourceException.class)
                    .extracting("code").isEqualTo("duplicate-email");
            verify(users, never()).save(any());
        }

        @Test
        void rejectsDuplicatedDocument() {
            when(users.existsByDocument("1020304050")).thenReturn(true);

            assertThatThrownBy(() -> new RegisterUserService(users, hasher).register(command))
                    .isInstanceOf(DuplicateResourceException.class)
                    .extracting("code").isEqualTo("duplicate-document");
        }

        @Test
        void rejectsNonCorporateEmail() {
            var gmail = new RegisterUserUseCase.Command("1", "Name", "someone@gmail.com", "Str0ngPass!", "S", "A");

            assertThatThrownBy(() -> new RegisterUserService(users, hasher).register(gmail))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting("code").isEqualTo("invalid-corporate-email");
        }
    }

    @Nested
    class Login {

        private final AuthenticateUserUseCase.Command command =
                new AuthenticateUserUseCase.Command("User1@helpflow.example", "secret");

        private AuthenticateUserService service() {
            return new AuthenticateUserService(users, hasher, tokens, metrics);
        }

        @Test
        void issuesATokenForValidCredentials() {
            User user = user(1, UserStatus.ACTIVO, Role.ROLE_USUARIO);
            when(users.findByEmail("user1@helpflow.example")).thenReturn(Optional.of(user));
            when(hasher.matches("secret", "hash")).thenReturn(true);
            when(tokens.issueFor(user)).thenReturn(new IssuedToken("jwt", Instant.EPOCH));

            var result = service().login(command);

            assertThat(result.token().value()).isEqualTo("jwt");
            assertThat(result.user()).isEqualTo(user);
        }

        @Test
        void unknownEmailAndWrongPasswordAreIndistinguishableAndCounted() {
            when(users.findByEmail("user1@helpflow.example")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service().login(command)).isInstanceOf(InvalidCredentialsException.class);

            when(users.findByEmail("user1@helpflow.example"))
                    .thenReturn(Optional.of(user(1, UserStatus.ACTIVO, Role.ROLE_USUARIO)));
            when(hasher.matches("secret", "hash")).thenReturn(false);

            assertThatThrownBy(() -> service().login(command)).isInstanceOf(InvalidCredentialsException.class);
            verify(metrics, org.mockito.Mockito.times(2)).authenticationFailed("bad_credentials");
            verify(tokens, never()).issueFor(any());
        }

        @Test
        void inactiveAccountsCannotLogInEvenWithTheRightPassword() {
            when(users.findByEmail("user1@helpflow.example"))
                    .thenReturn(Optional.of(user(1, UserStatus.INACTIVO, Role.ROLE_USUARIO)));
            when(hasher.matches("secret", "hash")).thenReturn(true);

            assertThatThrownBy(() -> service().login(command)).isInstanceOf(AccountInactiveException.class);
            verify(metrics).authenticationFailed("inactive_account");
        }
    }
}
