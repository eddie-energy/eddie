package energy.eddie.regionconnector.at.eda.permission.request;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.config.PlainAtConfiguration;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class PermissionRequestFactoryTest {
    @Test
    void testCreatePermissionRequest() {
        // Given
        StateBuilderFactory factory = new StateBuilderFactory(new PlainAtConfiguration("id", null), mock(EdaAdapter.class));
        PermissionRequestFactory permissionRequestFactory = new PermissionRequestFactory(Set.of(), factory);

        String connectionId = "connection123";
        String dataNeedId = "dataNeedId";
        CCMORequest ccmoRequest = mock(CCMORequest.class);

        // When
        PermissionRequest permissionRequest = permissionRequestFactory.create(connectionId, dataNeedId, ccmoRequest);

        // Then
        assertNotNull(permissionRequest);
    }
}