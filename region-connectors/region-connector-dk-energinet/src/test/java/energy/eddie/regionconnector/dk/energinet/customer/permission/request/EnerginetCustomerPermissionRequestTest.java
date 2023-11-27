package energy.eddie.regionconnector.dk.energinet.customer.permission.request;

import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.states.EnerginetCustomerRejectedState;
import energy.eddie.regionconnector.dk.energinet.enums.PeriodResolutionEnum;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class EnerginetCustomerPermissionRequestTest {
    @Test
    void constructor_setsParameters_andGeneratesPermissionId() {
        // Given
        String permissionId = "testPermissionId";
        String connectionId = "testConnectionId";
        String dataNeedId = "dataNeedId";
        var start = ZonedDateTime.now(EnerginetRegionConnector.DK_ZONE_ID).minusDays(10);
        var end = start.plusDays(1);
        String refreshToken = "refreshToken";
        String meteringPoint = "meteringPoint";
        PeriodResolutionEnum resolution = PeriodResolutionEnum.PT1H;
        EnerginetConfiguration config = mock(EnerginetConfiguration.class);

        // When
        var request = new EnerginetCustomerPermissionRequest(permissionId, connectionId, start, end,
                refreshToken, meteringPoint, dataNeedId, resolution, config);

        // Then
        assertEquals(permissionId, request.permissionId());
        assertEquals(connectionId, request.connectionId());
        assertEquals(start, request.start());
        assertEquals(end, request.end());
        assertEquals(refreshToken, request.refreshToken());
        assertEquals(meteringPoint, request.meteringPoint());
        assertEquals(dataNeedId, request.dataNeedId());
        assertEquals(resolution, request.periodResolution());
    }

    @Test
    void changeState_updatesState() {
        // Given
        String permissionId = "testPermissionId";
        String connectionId = "testConnectionId";
        String dataNeedId = "dataNeedId";
        var start = ZonedDateTime.now(EnerginetRegionConnector.DK_ZONE_ID).minusDays(10);
        var end = start.plusDays(1);
        String refreshToken = "refreshToken";
        String meteringPoint = "meteringPoint";
        PeriodResolutionEnum resolution = PeriodResolutionEnum.PT1H;
        EnerginetConfiguration config = mock(EnerginetConfiguration.class);

        // When
        var request = new EnerginetCustomerPermissionRequest(permissionId, connectionId, start, end,
                refreshToken, meteringPoint, dataNeedId, resolution, config);
        PermissionRequestState newState = new EnerginetCustomerRejectedState(request);

        // When
        request.changeState(newState);

        // Then
        assertEquals(newState, request.state());
    }
}
