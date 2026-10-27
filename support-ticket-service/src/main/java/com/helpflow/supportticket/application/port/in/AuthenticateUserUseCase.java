package com.helpflow.supportticket.application.port.in;

import com.helpflow.supportticket.application.model.IssuedToken;
import com.helpflow.supportticket.domain.model.User;

public interface AuthenticateUserUseCase {

    Result login(Command command);

    record Command(String email, String rawPassword) {
    }

    record Result(IssuedToken token, User user) {
    }
}
