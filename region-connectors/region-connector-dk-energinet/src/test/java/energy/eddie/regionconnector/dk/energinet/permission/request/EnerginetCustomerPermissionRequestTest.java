package energy.eddie.regionconnector.dk.energinet.permission.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.regionconnector.dk.DkEnerginetSpringConfig;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.permission.request.states.EnerginetCustomerRejectedState;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.DK_ZONE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class EnerginetCustomerPermissionRequestTest {
    @Test
    void constructor_setsParameters_andGeneratesPermissionId() {
        // Given
        String permissionId = "testPermissionId";
        String connectionId = "testConnectionId";
        String dataNeedId = "dataNeedId";
        var start = LocalDate.now(DK_ZONE_ID).minusDays(10);
        var end = start.plusDays(1);
        String refreshToken = "refreshToken";
        String meteringPoint = "meteringPoint";
        Granularity granularity = Granularity.PT1H;
        EnerginetCustomerApi apiClient = mock(EnerginetCustomerApi.class);
        ObjectMapper mapper = new DkEnerginetSpringConfig().objectMapper();
        var forCreation = new PermissionRequestForCreation(connectionId,
                refreshToken,
                meteringPoint,
                dataNeedId);

        // When
        var request = new EnerginetCustomerPermissionRequest(permissionId,
                forCreation,
                apiClient,
                start,
                end,
                granularity,
                                                             new StateBuilderFactory(),
                                                             mapper);

        // Then
        assertEquals(permissionId, request.permissionId());
        assertEquals(connectionId, request.connectionId());
        assertEquals(start, request.start());
        assertEquals(end, request.end());
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
        var start = LocalDate.now(DK_ZONE_ID).minusDays(10);
        var end = start.plusDays(1);
        String refreshToken = "refreshToken";
        String meteringPoint = "meteringPoint";
        Granularity granularity = Granularity.PT1H;
        EnerginetCustomerApi apiClient = mock(EnerginetCustomerApi.class);
        ObjectMapper mapper = new DkEnerginetSpringConfig().objectMapper();
        var forCreation = new PermissionRequestForCreation(connectionId,
                refreshToken,
                meteringPoint,
                dataNeedId);

        // When
        var request = new EnerginetCustomerPermissionRequest(permissionId,
                forCreation,
                apiClient,
                start,
                end,
                granularity,
                                                             new StateBuilderFactory(),
                                                             mapper);
        PermissionRequestState newState = new EnerginetCustomerRejectedState(request);

        // When
        request.changeState(newState);

        // Then
        assertEquals(newState, request.state());
    }
}
