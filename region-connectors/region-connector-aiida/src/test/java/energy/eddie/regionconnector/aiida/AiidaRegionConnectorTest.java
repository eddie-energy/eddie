package energy.eddie.regionconnector.aiida;

import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.aiida.services.AiidaRegionConnectorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiidaRegionConnectorTest {
    private final String expectedMdaCode = "aiida";
    private AiidaRegionConnector connector;
    @Mock
    private AiidaRegionConnectorService service;

    @BeforeEach
    void setUp() {
        connector = new AiidaRegionConnector(0, service);
    }

    @Test
    void getMetadata_MdaCodeIsAiida() {
        assertEquals(expectedMdaCode, connector.getMetadata().mdaCode());
        assertEquals("/region-connectors/aiida/", connector.getMetadata().urlPath());
    }

    @Test
    void verify_getStatusMessageFluxIsFromService() {
        connector.getConnectionStatusMessageStream();
        verify(service).connectionStatusMessageFlux();
    }

    @Test
    void verify_close_closesService() {
        connector.close();
        verify(service).close();
    }

    @Test
    void verify_healthStateIsAlwaysHealthy() {
        HealthState healthState = connector.health().get(expectedMdaCode);

        assertNotNull(healthState);
        assertEquals(HealthState.UP, healthState);
    }
}