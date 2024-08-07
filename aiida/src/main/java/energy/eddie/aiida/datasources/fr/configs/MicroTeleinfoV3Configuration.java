package energy.eddie.aiida.datasources.fr.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.aiida.datasources.AiidaDataSource;
import energy.eddie.aiida.datasources.api.DataSourceConfiguration;
import energy.eddie.aiida.datasources.fr.MicroTeleinfoV3;
import energy.eddie.aiida.utils.MqttConfig;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class MicroTeleinfoV3Configuration implements DataSourceConfiguration {
    private static final String CONFIG_PATH = "aiida.datasources.fr.teleinfo";
    private final ObjectMapper objectMapper;
    private final Set<AiidaDataSource> enabledDataSources = new HashSet<>();

    /**
     * Creates the configuration for the Oesterreichs Energie adapter.
     *
     * @param environment  Holds the environment variables of the configuration
     * @param objectMapper {@link ObjectMapper} that is used to deserialize the JSON messages.
     */
    public MicroTeleinfoV3Configuration(Environment environment, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        var binder = Binder.get(environment);
        var bindResult = binder.bind(CONFIG_PATH, Bindable.setOf(FrDataSourceConfig.class));
        if (bindResult.isBound()) {
            instantiateMicroTeleinfoV3FromConfig(bindResult.get());
        }
    }

    private void instantiateMicroTeleinfoV3FromConfig(Set<FrDataSourceConfig> configs) {
        var enabledConfigs = configs.stream()
                                    .filter(FrDataSourceConfig::enabled)
                                    .toList();
        enabledConfigs.forEach(config -> enabledDataSources.add(
                new MicroTeleinfoV3(
                        config.id(),
                        new MqttConfig.MqttConfigBuilder(
                                config.mqttServerUri(),
                                config.mqttSubscribeTopic() + "/" + config.meteringId()
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