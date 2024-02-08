package energy.eddie.regionconnector.fr.enedis.permission.request.models;

import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisDataSourceInformation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnector.ZONE_ID_FR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FutureDataPermissionTest {
    @Mock
    private PermissionRequestState permissionRequestState;

    @Test
    void gettersAndSettersTest() {
        // Given
        FutureDataPermission futureDataPermission = new FutureDataPermission();
        when(permissionRequestState.status()).thenReturn(PermissionProcessStatus.ACCEPTED);
        var today = ZonedDateTime.now(ZONE_ID_FR);

        // When
        futureDataPermission.withConnectionId("connectionId");
        futureDataPermission.withPermissionId("permissionId");
        futureDataPermission.withDataNeedId("dataNeedId");
        futureDataPermission.withMeteringPointId("meteringPointId");
        futureDataPermission.setLastPoll(ZonedDateTime.now());
        futureDataPermission.withState(permissionRequestState);
        futureDataPermission.withValidFrom(today.minusDays(2));
        futureDataPermission.withValidTo(today.minusDays(1));

        // Then
        assertEquals("connectionId", futureDataPermission.connectionId());
        assertEquals("permissionId", futureDataPermission.permissionId());
        assertEquals("dataNeedId", futureDataPermission.dataNeedId());
        assertEquals("meteringPointId", futureDataPermission.getMeteringPointId());
        assertEquals(permissionRequestState, futureDataPermission.state());
        assertEquals(today.minusDays(2), futureDataPermission.start());
        assertEquals(today.minusDays(1), futureDataPermission.end());
    }

    @Test
    void dataSourceInformationTest() {
        // Given
        FutureDataPermission futureDataPermission = new FutureDataPermission();

        // When
        DataSourceInformation dataSourceInformation = futureDataPermission.dataSourceInformation();

        // Then
        assertEquals(EnedisDataSourceInformation.class, dataSourceInformation.getClass());
    }
}
