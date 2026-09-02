// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.dto.validation;

import energy.eddie.regionconnector.at.eda.permission.request.dtos.PermissionRequestToImport;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class MeterReadingPairValidatorTest {
    private final MeterReadingPairValidator validator = new MeterReadingPairValidator();
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ConstraintValidatorContext context;

    @Test
    void givenBothMeterReadingDatesPresent_isValid() {
        // When
        var res = validator.isValid(request(day(1), day(2)), context);

        // Then
        assertTrue(res);
    }

    @Test
    void givenNoMeterReadingDates_isValid() {
        // When
        var res = validator.isValid(request(null, null), context);

        // Then
        assertTrue(res);
    }

    @Test
    void givenOnlyMeterReadingStart_isInvalid() {
        // When
        var res = validator.isValid(request(day(1), null), context);

        // Then
        assertFalse(res);
    }

    @Test
    void givenOnlyMeterReadingEnd_isInvalid() {
        // When
        var res = validator.isValid(request(null, day(2)), context);

        // Then
        assertFalse(res);
    }

    private static PermissionRequestToImport request(ZonedDateTime start, ZonedDateTime end) {
        return new PermissionRequestToImport(
                "cid",
                "AT0000000000000000000000000000000",
                "dnid",
                "AT001000",
                "Consent-ID",
                day(1),
                start,
                end
        );
    }

    private static ZonedDateTime day(int day) {
        return ZonedDateTime.parse("2026-01-%02dT00:00:00Z".formatted(day));
    }
}
