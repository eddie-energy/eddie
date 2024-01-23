package energy.eddie.regionconnector.aiida;

import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.aiida.services.AiidaRegionConnectorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiidaRegionConnectorTest {
    private final String expectedRcId = "aiida";
    @Mock
    private AiidaRegionConnectorService service;
    private AiidaRegionConnector connector;

    @BeforeEach
    void setUp() {
        connector = new AiidaRegionConnector(service);
    }

    @Test
    void getMetadata_MdaCodeIsAiida() {
        assertEquals(expectedRcId, connector.getMetadata().id());
    }

    @Test
    void verify_terminate_callsService() throws StateTransitionException {
        var permissionId = UUID.randomUUID().toString();
        connector.terminatePermission(permissionId);

        verify(service).terminatePermission(permissionId);
    }

    @Test
    void verify_healthStateIsAlwaysHealthy() {
        HealthState healthState = connector.health().get(expectedRcId);

        assertNotNull(healthState);
        assertEquals(HealthState.UP, healthState);
    }
}