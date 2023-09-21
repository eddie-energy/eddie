package energy.eddie.regionconnector.es.datadis;

import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.DataApi;
import energy.eddie.regionconnector.es.datadis.permission.request.InMemoryPermissionRequestRepository;
import energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class DatadisRegionConnectorTest {
    @Test
    void connectorThrows_ifDataApiNull() {
        var repo = new InMemoryPermissionRequestRepository();
        var authorizationApi = mock(AuthorizationApi.class);

        assertThrows(NullPointerException.class, () -> new DatadisRegionConnector(null, authorizationApi, repo));
    }

    @Test
    void connectorThrows_ifAuthorizationApiNull() {
        var repo = new InMemoryPermissionRequestRepository();
        var dataApi = mock(DataApi.class);

        assertThrows(NullPointerException.class, () -> new DatadisRegionConnector(dataApi, null, repo));
    }

    @Test
    void connectorThrows_ifPermissionRequestRepoNull() {
        var dataApi = mock(DataApi.class);
        var authorizationApi = mock(AuthorizationApi.class);

        assertThrows(NullPointerException.class, () -> new DatadisRegionConnector(dataApi, authorizationApi, null));
    }

    @Test
    void connectorConstructs() {
        var dataApi = mock(DataApi.class);
        var authorizationApi = mock(AuthorizationApi.class);
        var repo = new InMemoryPermissionRequestRepository();

        assertDoesNotThrow(() -> new DatadisRegionConnector(dataApi, authorizationApi, repo));
    }

    @Test
    void subscribeToConsumptionRecordPublisher_doesNotThrow() {

        var dataApi = mock(DataApi.class);
        var authorizationApi = mock(AuthorizationApi.class);
        var repo = new InMemoryPermissionRequestRepository();
        var connector = new DatadisRegionConnector(dataApi, authorizationApi, repo);

        assertDoesNotThrow(connector::getConsumptionRecordStream);
    }

    @Test
    void terminateNonExistingPermission_throwsIllegalStateException() {
        var dataApi = mock(DataApi.class);
        var authorizationApi = mock(AuthorizationApi.class);
        var repo = new InMemoryPermissionRequestRepository();
        var connector = new DatadisRegionConnector(dataApi, authorizationApi, repo);

        assertThrows(IllegalStateException.class, () -> connector.terminatePermission("permissionId"));
    }

    @Test
    void getMetadata_returnExpectedMetadata() {
        // given
        var dataApi = mock(DataApi.class);
        var authorizationApi = mock(AuthorizationApi.class);
        var repo = new InMemoryPermissionRequestRepository();
        var connector = new DatadisRegionConnector(dataApi, authorizationApi, repo);

        // when
        var result = connector.getMetadata();

        // then
        assertEquals(DatadisSpecificConstants.COUNTRY_CODE, result.countryCode());
        assertEquals(DatadisSpecificConstants.MDA_CODE, result.mdaCode());
        assertEquals(DatadisSpecificConstants.MDA_DISPLAY_NAME, result.mdaDisplayName());
        assertEquals(DatadisSpecificConstants.COVERED_METERING_POINTS, result.coveredMeteringPoints());
        assertEquals(DatadisSpecificConstants.BASE_PATH, result.urlPath());
    }

    @Test
    void health_returnsHealthChecks() {
        var dataApi = mock(DataApi.class);
        var authorizationApi = mock(AuthorizationApi.class);
        var repo = new InMemoryPermissionRequestRepository();
        var connector = new DatadisRegionConnector(dataApi, authorizationApi, repo);

        var res = connector.health();

        assertEquals(Map.of("permissionRequestRepository", HealthState.UP), res);
    }
}