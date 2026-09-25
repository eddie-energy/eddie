// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.dto.validation;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.DataNeedNotFoundResult;
import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.PermissionRequestToImport;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static energy.eddie.api.agnostic.Granularity.PT15M;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeterReadingDataNeedValidatorTest {
    private static final Timeframe TIMEFRAME = new Timeframe(LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 3));
    @Mock
    private DataNeedCalculationService calculationService;
    @InjectMocks
    private MeterReadingDataNeedValidator validator;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ConstraintValidatorContext context;

    @Test
    void givenNoMeterReadingDates_isValidWithoutCalculation() {
        // When
        var res = validator.isValid(request(day(1), null, null), context);

        // Then
        assertTrue(res);
        verifyNoInteractions(calculationService);
    }

    @Test
    void givenNonHistoricalDataNeedResult_isValid() {
        // Given
        when(calculationService.calculate("dnid", day(1))).thenReturn(new DataNeedNotFoundResult());

        // When
        var res = validator.isValid(request(day(1), day(1), day(2)), context);

        // Then
        assertTrue(res);
    }

    @Test
    void givenMeterReadingDatesWithinTimeframe_isValid() {
        // Given
        when(calculationService.calculate("dnid", day(1))).thenReturn(historicalResult());

        // When
        var res = validator.isValid(request(day(1), day(2), day(3)), context);

        // Then
        assertTrue(res);
    }

    @Test
    void givenMeterReadingStartBeforeTimeframe_isInvalid() {
        // Given
        when(calculationService.calculate("dnid", day(1))).thenReturn(historicalResult());

        // When
        var res = validator.isValid(request(day(1), day(1), day(3)), context);

        // Then
        assertFalse(res);
    }

    @Test
    void givenMeterReadingEndAfterTimeframe_isInvalid() {
        // Given
        when(calculationService.calculate("dnid", day(1))).thenReturn(historicalResult());

        // When
        var res = validator.isValid(request(day(1), day(2), day(4)), context);

        // Then
        assertFalse(res);
    }

    private static ValidatedHistoricalDataDataNeedResult historicalResult() {
        return new ValidatedHistoricalDataDataNeedResult(List.of(PT15M), TIMEFRAME, TIMEFRAME, null);
    }

    private static PermissionRequestToImport request(
            ZonedDateTime creationDateTime,
            ZonedDateTime start,
            ZonedDateTime end
    ) {
        return new PermissionRequestToImport(
                "cid",
                "AT0000000000000000000000000000000",
                "dnid",
                "AT001000",
                "Consent-ID",
                creationDateTime,
                start,
                end
        );
    }

    private static ZonedDateTime day(int day) {
        return ZonedDateTime.parse("2026-01-%02dT00:00:00Z".formatted(day));
    }
}
