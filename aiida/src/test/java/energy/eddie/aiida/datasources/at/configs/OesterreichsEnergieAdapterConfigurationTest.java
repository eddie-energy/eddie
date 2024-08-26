package energy.eddie.aiida.datasources.at.configs;

import energy.eddie.aiida.config.AiidaConfiguration;
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
        "aiida.datasources.at.oeas[0].id=oea1",
        "aiida.datasources.at.oeas[0].enabled=true",
        "aiida.datasources.at.oeas[0].mqttServerUri=tcp://test1",
        "aiida.datasources.at.oeas[0].mqttSubscribeTopic=test1",
        "aiida.datasources.at.oeas[0].mqttUsername=test1",
        "aiida.datasources.at.oeas[0].mqttPassword=test1",
        "aiida.datasources.at.oeas[1].id=oea2",
        "aiida.datasources.at.oeas[1].enabled=false",
        "aiida.datasources.at.oeas[1].mqttServerUri=tcp://test2",
        "aiida.datasources.at.oeas[1].mqttSubscribeTopic=test2",
        "aiida.datasources.at.oeas[1].mqttUsername=test2",
        "aiida.datasources.at.oeas[1].mqttPassword=test2",
        "aiida.datasources.at.oeas[2].id=oea1",
        "aiida.datasources.at.oeas[2].enabled=true",
        "aiida.datasources.at.oeas[2].mqttServerUri=tcp://test3",
        "aiida.datasources.at.oeas[2].mqttSubscribeTopic=test3",
        "aiida.datasources.at.oeas[2].mqttUsername=test3",
        "aiida.datasources.at.oeas[2].mqttPassword=test3",
})
class OesterreichsEnergieAdapterConfigurationTest {
    @Autowired
    private Environment environment;
    private OesterreichsEnergieAdapterConfiguration configuration;

    @BeforeEach
    public void setUp() {
        configuration = new OesterreichsEnergieAdapterConfiguration(environment,
                                                                    new AiidaConfiguration().objectMapper());
    }

    @Test
    void testEnabledDataSources() {
        Set<AiidaDataSource> enabledDataSources = configuration.enabledDataSources();
        assertEquals(1, enabledDataSources.size());

        var dataSource = enabledDataSources.stream().findFirst();
        assertTrue(dataSource.isPresent());
        assertEquals("oea1", dataSource.get().id());
    }
}
