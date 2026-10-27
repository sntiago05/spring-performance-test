package com.helpflow.dispatch.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.helpflow.dispatch.domain.model.Availability;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AvailabilityTest {

    @ParameterizedTest
    @CsvSource({"0,ALTA", "2,ALTA", "3,MEDIA", "8,MEDIA", "9,BAJA", "12,BAJA"})
    void classifiesEstimatedHours(int hours, Availability expected) {
        assertThat(Availability.fromEstimatedHours(hours)).isEqualTo(expected);
    }

    @Test
    void rejectsNegativeHours() {
        assertThatThrownBy(() -> Availability.fromEstimatedHours(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
