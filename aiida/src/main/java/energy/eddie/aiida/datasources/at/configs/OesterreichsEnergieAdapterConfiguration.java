package energy.eddie.aiida.datasources.at.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.datasources.AiidaDataSource;
import energy.eddie.aiida.datasources.api.DataSourceConfiguration;
import energy.eddie.aiida.datasources.at.OesterreichsEnergieAdapter;
import energy.eddie.aiida.utils.MqttConfig;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class OesterreichsEnergieAdapterConfiguration implements DataSourceConfiguration {
    private static final String CONFIG_PATH = "aiida.datasources.at.oeas";
    private final ObjectMapper objectMapper;
    private final Set<AiidaDataSource> enabledDataSources = new HashSet<>();

    public OesterreichsEnergieAdapterConfiguration(Environment environment, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        var binder = Binder.get(environment);
        var bindResult = binder.bind(CONFIG_PATH, Bindable.setOf(AtDataSourceConfig.class));
        if (bindResult.isBound()) {
            instantiateOesterreichsEnergieAdapterFromConfig(bindResult.get());
        }
    }

    private void instantiateOesterreichsEnergieAdapterFromConfig(Set<AtDataSourceConfig> configs) {
        var enabledConfigs = configs.stream()
                                    .filter(AtDataSourceConfig::enabled)
                                    .toList();
        enabledConfigs.forEach(config -> enabledDataSources.add(
                new OesterreichsEnergieAdapter(
                        config.id(),
                        new MqttConfig.MqttConfigBuilder(
                                config.mqttServerUri(),
                                config.mqttSubscribeTopic()
                        ).setUsername(config.mqttUsername())
                         .setPassword(config.mqttPassword())
                         .build(),
                        objectMapper
                )
        ));
    }

    @Override
    public Set<AiidaDataSource> enabledDataSources() {
        return enabledDataSources;
    }
}
