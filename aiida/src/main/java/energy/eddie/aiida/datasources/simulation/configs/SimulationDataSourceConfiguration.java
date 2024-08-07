package energy.eddie.aiida.datasources.simulation.configs;

import energy.eddie.aiida.datasources.AiidaDataSource;
import energy.eddie.aiida.datasources.api.DataSourceConfiguration;
import energy.eddie.aiida.datasources.simulation.SimulationDataSource;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

@Component
public class SimulationDataSourceConfiguration implements DataSourceConfiguration {
    private static final String CONFIG_PATH = "aiida.datasources.sim.simulations";
    private final Set<AiidaDataSource> enabledDataSources = new HashSet<>();
    private final Clock clock;

    public SimulationDataSourceConfiguration(Environment environment, Clock clock) {
        this.clock = clock;

        var binder = Binder.get(environment);
        var bindResult = binder.bind(CONFIG_PATH, Bindable.setOf(SimDataSourceConfig.class));
        if (bindResult.isBound()) {
            instantiateSimulationDataSourceFromConfig(bindResult.get());
        }
    }

    private void instantiateSimulationDataSourceFromConfig(Set<SimDataSourceConfig> configs) {
        var enabledConfigs = configs.stream()
                                    .filter(SimDataSourceConfig::enabled)
                                    .toList();
        enabledConfigs.forEach(config -> enabledDataSources.add(
                new SimulationDataSource(config.id(),
                                         clock,
                                         Duration.ofSeconds(config.simulationPeriodInSeconds()))
        ));
    }

    @Override
    public Set<AiidaDataSource> enabledDataSources() {
        return enabledDataSources;
    }
}
