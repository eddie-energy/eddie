package energy.eddie.regionconnector.dk.energinet.permission.request.validation;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetCustomerPermissionRequest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector.DK_ZONE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class NotAfterNowValidatorTest {
    private final NotAfterNowValidator validator = new NotAfterNowValidator();
    @Mock
    private EnerginetCustomerApi mockApiClient;

    @Test
    void givenStartIsInFuture_returnsError() {
        var now = ZonedDateTime.now(DK_ZONE_ID);
        var start = now.plusDays(10);
        var end = now.minusDays(10);

        var request = createTestRequest(start, end);
        var violations = validator.validate(request);
        assertEquals(1, violations.size());
    }

    @Test
    void givenEndIsInFuture_returnsError() {
        var now = ZonedDateTime.now(DK_ZONE_ID);
        var start = now.minusDays(10);
        var end = now.plusDays(10);

        var request = createTestRequest(start, end);
        var violations = validator.validate(request);
        assertEquals(1, violations.size());
    }

    @Test
    // once it has been finally decided how validation should be done, a clock reference should be passed to the validator
    @Disabled("Validator creates a ZonedDateTime.now() instance, which will always be after any passed instance, if no mock clock is used")
    void givenStartIsNow_passes() {
        var now = ZonedDateTime.now(DK_ZONE_ID);
        var end = now.minusDays(10);
        var request = createTestRequest(now, end);

        var violations = validator.validate(request);
        assertEquals(0, violations.size());
    }

    @Test
    @Disabled("Validator creates a ZonedDateTime.now() instance, which will always be after any passed instance, if no mock clock is used")
    void givenEndIsNow_passes() {
        var end = ZonedDateTime.now(DK_ZONE_ID);
        var start = end.minusDays(10);
        var request = createTestRequest(start, end);

        var violations = validator.validate(request);
        assertEquals(0, violations.size());
    }

    @Test
    @Disabled("Validator creates a ZonedDateTime.now() instance, which will always be after any passed instance, if no mock clock is used")
    void givenBothAreNow_passes() {
        var now = ZonedDateTime.now(DK_ZONE_ID);
        var request = createTestRequest(now, now);

        var violations = validator.validate(request);
        assertEquals(0, violations.size());
    }

    @Test
    void givenBothInPast_passes() {
        var now = ZonedDateTime.now(DK_ZONE_ID);
        var start = now.minusDays(10);
        var end = now.minusDays(5);
        var request = createTestRequest(start, end);

        var violations = validator.validate(request);
        assertEquals(0, violations.size());
    }

    @Test
    void givenBothInFuture_returnsError() {
        var now = ZonedDateTime.now(DK_ZONE_ID);
        var start = now.plusDays(5);
        var end = now.plusDays(10);
        var request = createTestRequest(start, end);

        var violations = validator.validate(request);
        assertEquals(1, violations.size());
    }

    private EnerginetCustomerPermissionRequest createTestRequest(ZonedDateTime start, ZonedDateTime end) {
        var forCreation = new PermissionRequestForCreation("bar", start, end, "too", Granularity.PT1H, "laa", "luu");

        return new EnerginetCustomerPermissionRequest("foo", forCreation, mockApiClient);
    }
}