package com.helpflow.supportticket.application.service;

import com.helpflow.supportticket.application.model.PageQuery;
import com.helpflow.supportticket.application.model.PageResult;
import com.helpflow.supportticket.application.model.UserSearchCriteria;
import com.helpflow.supportticket.application.port.in.ManageUserUseCase;
import com.helpflow.supportticket.application.port.out.UserRepositoryPort;
import com.helpflow.supportticket.application.port.out.TechnicianRepositoryPort;
import com.helpflow.supportticket.domain.exception.BusinessRuleViolationException;
import com.helpflow.supportticket.domain.exception.ForbiddenOperationException;
import com.helpflow.supportticket.domain.exception.ResourceNotFoundException;
import com.helpflow.supportticket.domain.model.Actor;
import com.helpflow.supportticket.domain.model.User;
import com.helpflow.supportticket.domain.model.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class ManageUserService implements ManageUserUseCase {

    private final UserRepositoryPort users;
    private final TechnicianRepositoryPort technicians;

    ManageUserService(UserRepositoryPort users, TechnicianRepositoryPort technicians) {
        this.users = users;
        this.technicians = technicians;
    }

    @Override
    @Transactional(readOnly = true)
    public User get(Actor actor, Long userId) {
        assertSelfOrAdmin(actor, userId);
        return load(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<User> list(UserSearchCriteria criteria, PageQuery page) {
        return users.search(criteria, page);
    }

    @Override
    public User updateProfile(Actor actor, Long userId, ProfileCommand command) {
        assertSelfOrAdmin(actor, userId);
        return users.save(load(userId).withProfile(command.fullName().trim(), command.site().trim(),
                command.area().trim()));
    }

    @Override
    public User changeStatus(Actor actor, Long userId, UserStatus status) {
        if (actor.userId().equals(userId) && status == UserStatus.INACTIVO) {
            throw new BusinessRuleViolationException("cannot-deactivate-self",
                    "User " + userId + " tried to deactivate their own account");
        }
        User updated = users.save(load(userId).withStatus(status));
        technicians.findByUserId(userId).ifPresent(technician -> technicians.save(technician.withProfile(
                technician.fullName(), technician.specialty(), technician.site(), technician.maxActiveTickets(),
                status)));
        return updated;
    }

    private User load(Long userId) {
        return users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("user-not-found",
                "User " + userId + " not found", userId));
    }

    private void assertSelfOrAdmin(Actor actor, Long userId) {
        if (!actor.isAdmin() && !actor.userId().equals(userId)) {
            throw new ForbiddenOperationException("user-access-denied",
                    "User " + actor.userId() + " cannot access user " + userId);
        }
    }
}
