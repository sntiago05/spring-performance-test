package com.helpflow.supportticket.application.service;

import com.helpflow.supportticket.application.model.PageQuery;
import com.helpflow.supportticket.application.model.PageResult;
import com.helpflow.supportticket.application.port.in.ManageTechnicianUseCase;
import com.helpflow.supportticket.application.port.in.RegisterUserUseCase;
import com.helpflow.supportticket.application.port.out.PasswordHasherPort;
import com.helpflow.supportticket.application.port.out.TechnicianRepositoryPort;
import com.helpflow.supportticket.application.port.out.UserRepositoryPort;
import com.helpflow.supportticket.domain.exception.ResourceNotFoundException;
import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.Role;
import com.helpflow.supportticket.domain.model.Technician;
import com.helpflow.supportticket.domain.model.User;
import com.helpflow.supportticket.domain.model.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class ManageTechnicianService implements ManageTechnicianUseCase {

    private static final Logger log = LoggerFactory.getLogger(ManageTechnicianService.class);

    private final TechnicianRepositoryPort technicians;
    private final UserRepositoryPort users;
    private final PasswordHasherPort passwordHasher;

    ManageTechnicianService(TechnicianRepositoryPort technicians, UserRepositoryPort users,
            PasswordHasherPort passwordHasher) {
        this.technicians = technicians;
        this.users = users;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public Technician create(CreateCommand command) {
        User account = users.save(RegisterUserService.newUser(
                new RegisterUserUseCase.Command(command.document(), command.fullName(), command.email(),
                        command.rawPassword(), command.site(), command.area()),
                Role.ROLE_TECNICO, users, passwordHasher));
        Technician created = technicians.save(new Technician(null, technicians.nextCode(), account.id(),
                account.fullName(), command.specialty(), command.site().trim(), command.maxActiveTickets(),
                UserStatus.ACTIVO));
        log.info("Technician {} created for user {}", created.code(), account.id());
        return created;
    }

    @Override
    @Transactional(readOnly = true)
    public Technician get(Long technicianId) {
        return load(technicianId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Technician> list(UserStatus status, Category specialty, PageQuery page) {
        return technicians.search(status, specialty, page);
    }

    @Override
    public Technician update(Long technicianId, UpdateCommand command) {
        Technician updated = load(technicianId).withProfile(command.fullName().trim(), command.specialty(),
                command.site().trim(), command.maxActiveTickets(), command.status());
        return technicians.save(updated);
    }

    private Technician load(Long technicianId) {
        return technicians.findById(technicianId).orElseThrow(() -> new ResourceNotFoundException(
                "technician-not-found", "Technician " + technicianId + " not found", technicianId));
    }
}
