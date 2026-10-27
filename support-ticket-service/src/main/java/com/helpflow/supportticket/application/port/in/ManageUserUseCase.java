package com.helpflow.supportticket.application.port.in;

import com.helpflow.supportticket.application.model.PageQuery;
import com.helpflow.supportticket.application.model.PageResult;
import com.helpflow.supportticket.application.model.UserSearchCriteria;
import com.helpflow.supportticket.domain.model.Actor;
import com.helpflow.supportticket.domain.model.User;
import com.helpflow.supportticket.domain.model.UserStatus;

public interface ManageUserUseCase {

    /**
     * Admins can read any user, everybody else only their own record.
     */
    User get(Actor actor, Long userId);

    PageResult<User> list(UserSearchCriteria criteria, PageQuery page);

    /**
     * Admins can edit any user, everybody else only their own basic information.
     */
    User updateProfile(Actor actor, Long userId, ProfileCommand command);

    /**
     * Activates or deactivates an account. Admins cannot deactivate themselves, and the support profile of a
     * technician follows the status of its account so an inactive technician never receives tickets.
     */
    User changeStatus(Actor actor, Long userId, UserStatus status);

    record ProfileCommand(String fullName, String site, String area) {
    }
}
