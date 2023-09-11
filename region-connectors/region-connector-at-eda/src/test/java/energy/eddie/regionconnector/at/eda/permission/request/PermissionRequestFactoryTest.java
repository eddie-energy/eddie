package energy.eddie.regionconnector.at.eda.permission.request;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class PermissionRequestFactoryTest {
    @Test
    void testCreatePermissionRequest() {
        // Given
        EdaAdapter edaAdapterMock = mock(EdaAdapter.class);
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        AtPermissionRequestRepository permissionRequestRepository = new InMemoryPermissionRequestRepository();
        PermissionRequestFactory permissionRequestFactory = new PermissionRequestFactory(edaAdapterMock, permissionStateMessages, permissionRequestRepository);

        String connectionId = "connection123";
        CCMORequest ccmoRequest = mock(CCMORequest.class);

        // When
        PermissionRequest permissionRequest = permissionRequestFactory.create(connectionId, ccmoRequest);

        // Then
        assertNotNull(permissionRequest);
    }
}