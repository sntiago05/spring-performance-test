package com.helpflow.supportticket.application.port.out;

import com.helpflow.supportticket.application.model.IssuedToken;
import com.helpflow.supportticket.domain.model.User;

public interface TokenIssuerPort {

    IssuedToken issueFor(User user);
}
