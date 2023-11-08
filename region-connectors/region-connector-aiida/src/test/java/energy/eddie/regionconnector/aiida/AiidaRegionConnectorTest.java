package energy.eddie.regionconnector.aiida;

import energy.eddie.api.v0.HealthState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AiidaRegionConnectorTest {
    private final String expectedMdaCode = "aiida";
    private AiidaRegionConnector connector;

    @BeforeEach
    void setUp() {
        connector = new AiidaRegionConnector(0);
    }

    @Test
    void getMetadata_MdaCodeIsAiida() {
        assertEquals(expectedMdaCode, connector.getMetadata().mdaCode());
        assertEquals("/region-connectors/aiida/", connector.getMetadata().urlPath());
    }

    @Test
    void verify_getConsumptionRecordStream_throws() {
        assertThrows(UnsupportedOperationException.class, () -> connector.getConsumptionRecordStream());
    }

    @Test
    void verify_healthStateIsAlwaysHealthy() {
        HealthState healthState = connector.health().get(expectedMdaCode);

        assertNotNull(healthState);
        assertEquals(HealthState.UP, healthState);
    }
}