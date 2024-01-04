package energy.eddie.regionconnector.dk.energinet.customer.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.states.EnerginetCustomerRejectedState;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
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
        Granularity granularity = Granularity.PT1H;
        EnerginetCustomerApi apiClient = mock(EnerginetCustomerApi.class);
        var forCreation = new PermissionRequestForCreation(connectionId, start, end, refreshToken, granularity, meteringPoint, dataNeedId);

        // When
        var request = new EnerginetCustomerPermissionRequest(permissionId, forCreation, apiClient);

        // Then
        assertEquals(permissionId, request.permissionId());
        assertEquals(connectionId, request.connectionId());
        assertEquals(start, request.start());
        assertEquals(end, request.end());
        assertEquals(refreshToken, request.refreshToken());
        assertEquals(meteringPoint, request.meteringPoint());
        assertEquals(dataNeedId, request.dataNeedId());
        assertEquals(granularity, request.granularity());
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
        Granularity granularity = Granularity.PT1H;
        EnerginetCustomerApi apiClient = mock(EnerginetCustomerApi.class);
        var forCreation = new PermissionRequestForCreation(connectionId, start, end, refreshToken, granularity, meteringPoint, dataNeedId);

        // When
        var request = new EnerginetCustomerPermissionRequest(permissionId, forCreation, apiClient);
        PermissionRequestState newState = new EnerginetCustomerRejectedState(request);

        // When
        request.changeState(newState);

        // Then
        assertEquals(newState, request.state());
    }
}