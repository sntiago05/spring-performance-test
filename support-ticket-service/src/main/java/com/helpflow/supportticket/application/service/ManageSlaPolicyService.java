package com.helpflow.supportticket.application.service;

import com.helpflow.supportticket.application.port.in.ManageSlaPolicyUseCase;
import com.helpflow.supportticket.application.port.out.SlaPolicyRepositoryPort;
import com.helpflow.supportticket.domain.model.Priority;
import com.helpflow.supportticket.domain.model.SlaPolicy;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class ManageSlaPolicyService implements ManageSlaPolicyUseCase {

    private final SlaPolicyRepositoryPort policies;

    ManageSlaPolicyService(SlaPolicyRepositoryPort policies) {
        this.policies = policies;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SlaPolicy> list() {
        return policies.findAll();
    }

    @Override
    public SlaPolicy update(Priority priority, int resolutionHours, int escalationThresholdPercent) {
        return policies.save(new SlaPolicy(priority, resolutionHours, escalationThresholdPercent));
    }
}
