package energy.eddie.regionconnector.dk.energinet.customer.permission.request.states;

import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.EnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.enums.PeriodResolutionEnum;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class EnerginetCustomerPendingAcknowledgmentStateTest {
    @Test
    void receivedPermissionAdminAnswer_transitionsState() {
        // Given
        var start = ZonedDateTime.now(ZoneId.of("Europe/Copenhagen")).minusDays(10);
        String permissionId = UUID.randomUUID().toString();
        String refreshToken = "refreshToken";
        String meteringPoint = "meteringPoint";
        PeriodResolutionEnum resolution = PeriodResolutionEnum.PT1H;
        String connectionId = "cid";
        String dataNeedId = "dataNeedId";
        EnerginetConfiguration config = mock(EnerginetConfiguration.class);

        var permissionRequest = new EnerginetCustomerPermissionRequest(permissionId, connectionId, start,
                start.plusDays(5), refreshToken, meteringPoint, dataNeedId, resolution, config);
        EnerginetCustomerPendingAcknowledgmentState state = new EnerginetCustomerPendingAcknowledgmentState(permissionRequest);

        // When
        state.receivedPermissionAdministratorResponse();

        // Then
        assertEquals(EnerginetCustomerSentToPermissionAdministratorState.class, permissionRequest.state().getClass());
    }
}
