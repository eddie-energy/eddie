package energy.eddie.aiida.datasources.simulation.configs;

import energy.eddie.aiida.datasources.AiidaDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Clock;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "aiida.datasources.sim.simulations[0].id=sim1",
        "aiida.datasources.sim.simulations[0].enabled=true",
        "aiida.datasources.sim.simulations[0].simulationPeriodInSeconds=5",
        "aiida.datasources.sim.simulations[1].id=sim2",
        "aiida.datasources.sim.simulations[1].enabled=false",
        "aiida.datasources.sim.simulations[1].simulationPeriodInSeconds=3",
        "aiida.datasources.sim.simulations[2].id=sim1",
        "aiida.datasources.sim.simulations[2].enabled=true",
        "aiida.datasources.sim.simulations[2].simulationPeriodInSeconds=3"
})
class SimulationDataSourceConfigurationTest {
    Clock clock = Clock.systemUTC();
    @Autowired
    private Environment environment;
    private SimulationDataSourceConfiguration configuration;

    @BeforeEach
    public void setUp() {
        configuration = new SimulationDataSourceConfiguration(environment, clock);
    }

    @Test
    void testEnabledDataSources() {
        // Verify that only enabled data sources are added
        Set<AiidaDataSource> enabledDataSources = configuration.enabledDataSources();
        assertEquals(1, enabledDataSources.size());

        var dataSource = enabledDataSources.stream().findFirst();
        assertTrue(dataSource.isPresent());
        assertEquals("sim1", dataSource.get().id());
    }
}
