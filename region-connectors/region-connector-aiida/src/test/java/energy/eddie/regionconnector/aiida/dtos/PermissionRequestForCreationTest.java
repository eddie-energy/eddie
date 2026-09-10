// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.dtos;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionRequestForCreationTest {
    private Validator validator;

    @BeforeEach
    void setUp() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void givenNoLimitDefaults_isValid() {
        var request = new PermissionRequestForCreation("conn-1", List.of("data-need-1"), "meter-1", null, null);

        var violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void givenMinLessThanMax_isValid() {
        var request = new PermissionRequestForCreation("conn-1",
                                                        List.of("data-need-1"),
                                                        "meter-1",
                                                        BigDecimal.ONE,
                                                        BigDecimal.TEN);

        var violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void givenMinEqualToMax_isInvalid() {
        var request = new PermissionRequestForCreation("conn-1",
                                                        List.of("data-need-1"),
                                                        "meter-1",
                                                        BigDecimal.TEN,
                                                        BigDecimal.TEN);

        var violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void givenMinGreaterThanMax_isInvalid() {
        var request = new PermissionRequestForCreation("conn-1",
                                                        List.of("data-need-1"),
                                                        "meter-1",
                                                        BigDecimal.TEN,
                                                        BigDecimal.ONE);

        var violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }
}
