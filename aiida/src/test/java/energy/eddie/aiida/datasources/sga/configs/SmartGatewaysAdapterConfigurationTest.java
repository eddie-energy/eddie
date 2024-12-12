package energy.eddie.aiida.datasources.sga.configs;

import energy.eddie.aiida.datasources.AiidaDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "aiida.datasources.sga[0].id=sga1",
        "aiida.datasources.sga[0].enabled=true",
        "aiida.datasources.sga[0].mqttServerUri=tcp://test1",
        "aiida.datasources.sga[0].mqttSubscribeTopic=test1",
        "aiida.datasources.sga[0].mqttUsername=test1",
        "aiida.datasources.sga[0].mqttPassword=test1",
        "aiida.datasources.sga[1].id=sga2",
        "aiida.datasources.sga[1].enabled=false",
        "aiida.datasources.sga[1].mqttServerUri=tcp://test2",
        "aiida.datasources.sga[1].mqttSubscribeTopic=test2",
        "aiida.datasources.sga[1].mqttUsername=test2",
        "aiida.datasources.sga[1].mqttPassword=test2",
})
class SmartGatewaysAdapterConfigurationTest {
    @Autowired
    private Environment environment;
    private SmartGatewaysAdapterConfiguration configuration;

    @BeforeEach
    public void setUp() {
        configuration = new SmartGatewaysAdapterConfiguration(environment);
    }

    @Test
    void testEnabledDataSources() {
        Set<AiidaDataSource> enabledDataSources = configuration.enabledDataSources();
        assertEquals(1, enabledDataSources.size());

        var dataSource = enabledDataSources.stream().findFirst();
        assertTrue(dataSource.isPresent());
        assertEquals("sga1", dataSource.get().id());
    }
}
