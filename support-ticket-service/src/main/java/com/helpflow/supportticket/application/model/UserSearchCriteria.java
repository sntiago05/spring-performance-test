package com.helpflow.supportticket.application.model;

import com.helpflow.supportticket.domain.model.Role;
import com.helpflow.supportticket.domain.model.UserStatus;

public record UserSearchCriteria(UserStatus status, Role role, String site) {
}
