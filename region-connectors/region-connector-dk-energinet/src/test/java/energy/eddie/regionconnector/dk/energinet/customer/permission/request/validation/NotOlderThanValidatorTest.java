package energy.eddie.regionconnector.dk.energinet.customer.permission.request.validation;

import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.EnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.enums.PeriodResolutionEnum;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
// Dedicated test methods are clearer and the assertions differ
@SuppressWarnings("java:S5976")
class NotOlderThanValidatorTest {
    private final NotOlderThanValidator validator = new NotOlderThanValidator(ChronoUnit.MONTHS, 24);
    @Mock
    private EnerginetConfiguration mockConfig;

    @Test
    void givenOlderStart_returnsError() {
        var now = ZonedDateTime.now();
        var start = now.minusMonths(30);
        var end = now.minusDays(10);

        var request = createTestRequest(start, end);
        var violations = validator.validate(request);
        assertEquals(1, violations.size());
    }

    @Test
    @Disabled("Will always fail, because the validator creates a ZonedDateTime.now(), which will always be newer than the passed now value")
    void givenStartEqualToLimit_passes() {
        var now = ZonedDateTime.now();
        var start = now.minusMonths(24);
        var end = now.minusDays(10);

        var request = createTestRequest(start, end);
        var violations = validator.validate(request);
        assertEquals(0, violations.size());
    }

    @Test
    void givenStartBelowLimit_passes() {
        var now = ZonedDateTime.now();
        var start = now.minusMonths(10);
        var end = now.minusDays(10);

        var request = createTestRequest(start, end);
        var violations = validator.validate(request);
        assertEquals(0, violations.size());
    }

    private EnerginetCustomerPermissionRequest createTestRequest(ZonedDateTime start, ZonedDateTime end) {
        return new EnerginetCustomerPermissionRequest("foo", "bar", start, end,
                "too", "laa", "luu", PeriodResolutionEnum.PT1H, mockConfig);
    }
}