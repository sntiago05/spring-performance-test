package com.helpflow.supportticket.application.port.in;

import com.helpflow.supportticket.domain.model.Priority;
import com.helpflow.supportticket.domain.model.SlaPolicy;
import java.util.List;

public interface ManageSlaPolicyUseCase {

    List<SlaPolicy> list();

    /**
     * Changes apply to tickets created afterwards; existing tickets keep their deadline.
     */
    SlaPolicy update(Priority priority, int resolutionHours, int escalationThresholdPercent);
}
