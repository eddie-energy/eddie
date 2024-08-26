package energy.eddie.aiida.datasources.fr.configs;

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
        "aiida.datasources.fr.teleinfo[0].id=teleinfo1",
        "aiida.datasources.fr.teleinfo[0].enabled=true",
        "aiida.datasources.fr.teleinfo[0].mqttServerUri=tcp://test1",
        "aiida.datasources.fr.teleinfo[0].mqttSubscribeTopic=test1",
        "aiida.datasources.fr.teleinfo[0].mqttUsername=test1",
        "aiida.datasources.fr.teleinfo[0].mqttPassword=test1",
        "aiida.datasources.fr.teleinfo[0].meteringId=1234",
        "aiida.datasources.fr.teleinfo[1].id=teleinfo2",
        "aiida.datasources.fr.teleinfo[1].enabled=false",
        "aiida.datasources.fr.teleinfo[1].mqttServerUri=tcp://test2",
        "aiida.datasources.fr.teleinfo[1].mqttSubscribeTopic=test2",
        "aiida.datasources.fr.teleinfo[1].mqttUsername=test2",
        "aiida.datasources.fr.teleinfo[1].mqttPassword=test2",
        "aiida.datasources.fr.teleinfo[1].meteringId=1234",
        "aiida.datasources.fr.teleinfo[2].id=teleinfo1",
        "aiida.datasources.fr.teleinfo[2].enabled=true",
        "aiida.datasources.fr.teleinfo[2].mqttServerUri=tcp://test3",
        "aiida.datasources.fr.teleinfo[2].mqttSubscribeTopic=test3",
        "aiida.datasources.fr.teleinfo[2].mqttUsername=test3",
        "aiida.datasources.fr.teleinfo[2].mqttPassword=test3",
        "aiida.datasources.fr.teleinfo[2].meteringId=1234",
})
class MicroTeleinfoV3ConfigurationTest {
    @Autowired
    private Environment environment;
    private MicroTeleinfoV3Configuration configuration;

    @BeforeEach
    public void setUp() {
        configuration = new MicroTeleinfoV3Configuration(environment, new AiidaConfiguration().objectMapper());
    }

    @Test
    void testEnabledDataSources() {
        Set<AiidaDataSource> enabledDataSources = configuration.enabledDataSources();
        assertEquals(1, enabledDataSources.size());

        var dataSource = enabledDataSources.stream().findFirst();
        assertTrue(dataSource.isPresent());
        assertEquals("teleinfo1", dataSource.get().id());
    }
}
