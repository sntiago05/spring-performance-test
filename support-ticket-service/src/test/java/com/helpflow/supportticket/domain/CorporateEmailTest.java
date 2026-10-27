package com.helpflow.supportticket.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.helpflow.supportticket.domain.model.CorporateEmail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CorporateEmailTest {

    @ParameterizedTest
    @ValueSource(strings = {"ana@helpflow.example", "ana.perez+it@empresa.co", "a_b-c@sub.empresa.com"})
    void acceptsCorporateAddresses(String email) {
        assertThat(CorporateEmail.isValid(email)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ana@gmail.com", "ana@HOTMAIL.com", "ana@outlook.com", "ana@yahoo.com", "ana@icloud.com"})
    void rejectsPublicMailboxProviders(String email) {
        assertThat(CorporateEmail.isValid(email)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "plainaddress", "@empresa.com", "ana@", "ana@empresa", "ana @empresa.com", "ana@@empresa.com"})
    void rejectsMalformedAddresses(String email) {
        assertThat(CorporateEmail.isValid(email)).isFalse();
    }

    @Test
    void nullIsInvalidAndNormalizationLowercases() {
        assertThat(CorporateEmail.isValid(null)).isFalse();
        assertThat(CorporateEmail.normalize("  Ana@HelpFlow.Example ")).isEqualTo("ana@helpflow.example");
    }
}
