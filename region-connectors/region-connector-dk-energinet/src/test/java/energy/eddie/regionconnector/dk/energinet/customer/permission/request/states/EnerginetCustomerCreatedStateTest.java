package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.api.v0.process.model.validation.ValidationException;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.EnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.enums.PeriodResolutionEnum;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class EnerginetCustomerCreatedStateTest {
    @Test
    void validate_changesToValidatedState_whenValid() {
        // Given
        var start = ZonedDateTime.now(ZoneId.of("Europe/Copenhagen")).minusDays(10);
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        String meteringPoint = "meteringPoint";
        PeriodResolutionEnum resolution = PeriodResolutionEnum.PT1H;
        String connectionId = "cid";
        String dataNeedId = "dataNeedId";
        EnerginetConfiguration config = mock(EnerginetConfiguration.class);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId, connectionId, start, start.plusDays(1),
                refreshToken, meteringPoint, dataNeedId, resolution, config);

        // When
        assertDoesNotThrow(permissionRequest::validate);

        // Then
        assertEquals(EnerginetCustomerValidatedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToMalformedState_whenConnectionIdNull() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(ZoneId.of("Europe/Copenhagen"));
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        String meteringPoint = "meteringPoint";
        PeriodResolutionEnum resolution = PeriodResolutionEnum.PT1H;
        String dataNeedId = "dataNeedId";
        EnerginetConfiguration config = mock(EnerginetConfiguration.class);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId, null,
                start, start.plusDays(1), refreshToken, meteringPoint, dataNeedId, resolution, config);

        // When
        assertThrows(ValidationException.class, permissionRequest::validate);

        // Then
        assertEquals(EnerginetCustomerMalformedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToMalformedState_whenConnectionIdBlank() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(ZoneId.of("Europe/Copenhagen"));
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        String meteringPoint = "meteringPoint";
        PeriodResolutionEnum resolution = PeriodResolutionEnum.PT1H;
        String dataNeedId = "dataNeedId";
        EnerginetConfiguration config = mock(EnerginetConfiguration.class);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId, " ",
                start, start.plusDays(1), refreshToken, meteringPoint, dataNeedId, resolution, config);

        // When
        assertThrows(ValidationException.class, permissionRequest::validate);

        // Then
        assertEquals(EnerginetCustomerMalformedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToMalformedState_whenStartNull() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(ZoneId.of("Europe/Copenhagen"));
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        String connectionId = "cid";
        String meteringPoint = "meteringPoint";
        PeriodResolutionEnum resolution = PeriodResolutionEnum.PT1H;
        String dataNeedId = "dataNeedId";
        EnerginetConfiguration config = mock(EnerginetConfiguration.class);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId, connectionId,
                null, start.plusDays(1), refreshToken, meteringPoint, dataNeedId, resolution, config);

        // When
        assertThrows(ValidationException.class, permissionRequest::validate);

        // Then
        assertEquals(EnerginetCustomerMalformedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToMalformedState_whenEndNull() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(ZoneId.of("Europe/Copenhagen"));
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        String connectionId = "cid";
        String meteringPoint = "meteringPoint";
        PeriodResolutionEnum resolution = PeriodResolutionEnum.PT1H;
        String dataNeedId = "dataNeedId";
        EnerginetConfiguration config = mock(EnerginetConfiguration.class);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId, connectionId,
                start, null, refreshToken, meteringPoint, dataNeedId, resolution, config);

        // When
        assertThrows(ValidationException.class, permissionRequest::validate);

        // Then
        assertEquals(EnerginetCustomerMalformedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToMalformedState_whenEndBeforeStart() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(ZoneId.of("Europe/Copenhagen"));
        var end = start.minusDays(10);
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        String connectionId = "cid";
        String meteringPoint = "meteringPoint";
        PeriodResolutionEnum resolution = PeriodResolutionEnum.PT1H;
        String dataNeedId = "dataNeedId";
        EnerginetConfiguration config = mock(EnerginetConfiguration.class);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId, connectionId,
                start, end, refreshToken, meteringPoint, dataNeedId, resolution, config);

        // When
        assertThrows(ValidationException.class, permissionRequest::validate);

        // Then
        assertEquals(EnerginetCustomerMalformedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToMalformedState_whenRefreshTokenBlank() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(ZoneId.of("Europe/Copenhagen"));
        String permissionId = UUID.randomUUID().toString();
        String meteringPoint = "meteringPoint";
        PeriodResolutionEnum resolution = PeriodResolutionEnum.PT1H;
        String connectionId = "cid";
        String dataNeedId = "dataNeedId";
        EnerginetConfiguration config = mock(EnerginetConfiguration.class);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId, connectionId, start, start.plusDays(1),
                "", meteringPoint, dataNeedId, resolution, config);

        // When
        assertThrows(ValidationException.class, permissionRequest::validate);

        // Then
        assertEquals(EnerginetCustomerMalformedState.class, permissionRequest.state().getClass());
    }

    @Test
    void validate_changesToMalformedState_whenMeteringPointBlank() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(ZoneId.of("Europe/Copenhagen"));
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        PeriodResolutionEnum resolution = PeriodResolutionEnum.PT1H;
        String connectionId = "cid";
        String dataNeedId = "dataNeedId";
        EnerginetConfiguration config = mock(EnerginetConfiguration.class);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId, connectionId, start, start.plusDays(1),
                refreshToken, " ", dataNeedId, resolution, config);

        // When
        assertThrows(ValidationException.class, permissionRequest::validate);

        // Then
        assertEquals(EnerginetCustomerMalformedState.class, permissionRequest.state().getClass());
    }
}