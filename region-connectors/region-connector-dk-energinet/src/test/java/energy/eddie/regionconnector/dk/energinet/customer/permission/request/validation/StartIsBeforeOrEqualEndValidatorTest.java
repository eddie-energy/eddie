package energy.eddie.regionconnector.dk.energinet.customer.permission.request.validation;

import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.EnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.enums.PeriodResolutionEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class StartIsBeforeOrEqualEndValidatorTest {
    private final StartIsBeforeOrEqualEndValidator validator = new StartIsBeforeOrEqualEndValidator();
    @Mock
    private EnerginetConfiguration mockConfig;

    @Test
    void givenStartBeforeEnd_passes() {
        var now = ZonedDateTime.now();
        var start = now.minusDays(10);
        var end = now.minusDays(5);

        var request = createTestRequest(start, end);
        var violations = validator.validate(request);
        assertEquals(0, violations.size());
    }

    @Test
    void givenStartEqualToEnd_passes() {
        var now = ZonedDateTime.now();
        var start = now.minusDays(10);
        var end = start;

        var request = createTestRequest(start, end);
        var violations = validator.validate(request);
        assertEquals(0, violations.size());
    }

    @Test
    void givenStartAfterEnd_returnsError() {
        var now = ZonedDateTime.now();
        var start = now.minusDays(10);
        var end = now.minusDays(15);

        var request = createTestRequest(start, end);
        var violations = validator.validate(request);
        assertEquals(1, violations.size());
    }

    private EnerginetCustomerPermissionRequest createTestRequest(ZonedDateTime start, ZonedDateTime end) {
        return new EnerginetCustomerPermissionRequest("foo", "bar", start, end,
                "too", "laa", "luu", PeriodResolutionEnum.PT1H, mockConfig);
    }
}