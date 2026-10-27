package com.helpflow.supportticket.domain;

import static com.helpflow.supportticket.support.Fixtures.technician;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.Priority;
import com.helpflow.supportticket.domain.model.Technician;
import org.junit.jupiter.api.Test;

class TechnicianTest {

    @Test
    void handlesItsOwnSpecialtyAndGeneralistsHandleEverything() {
        Technician network = technician(1, Category.RED, "BOGOTA", 3);
        Technician generalist = technician(2, Category.OTRO, "BOGOTA", 3);

        assertThat(network.canHandle(Category.RED)).isTrue();
        assertThat(network.canHandle(Category.SOFTWARE)).isFalse();
        assertThat(network.canHandle(Category.OTRO)).isTrue();
        assertThat(generalist.canHandle(Category.HARDWARE)).isTrue();
    }

    @Test
    void capacityIsTheMaximumOfActiveTicketsExceptForCriticalOnes() {
        Technician technician = technician(1, Category.RED, "BOGOTA", 3);

        assertThat(technician.hasCapacityFor(Priority.ALTA, 2)).isTrue();
        assertThat(technician.hasCapacityFor(Priority.ALTA, 3)).isFalse();
        assertThat(technician.hasCapacityFor(Priority.CRITICA, 3)).isTrue();
        assertThat(technician.hasCapacityFor(Priority.CRITICA, 4)).isFalse();
    }

    @Test
    void siteComparisonIgnoresCaseAndSurroundingSpaces() {
        Technician technician = technician(1, Category.RED, "Bogota", 3);

        assertThat(technician.isOnSite(" BOGOTA ")).isTrue();
        assertThat(technician.isOnSite("CALI")).isFalse();
        assertThat(technician.isOnSite(null)).isFalse();
    }

    @Test
    void capacityMustBePositive() {
        assertThatThrownBy(() -> technician(1, Category.RED, "BOGOTA", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
