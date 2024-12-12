package energy.eddie.aiida.datasources.sga.configs;

import energy.eddie.aiida.datasources.AiidaDataSource;
import energy.eddie.aiida.datasources.api.DataSourceConfiguration;
import energy.eddie.aiida.datasources.sga.SmartGatewaysAdapter;
import energy.eddie.aiida.datasources.sga.configs.dtos.SmartGatewaysAdapterDatasource;
import energy.eddie.aiida.utils.MqttConfig;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class SmartGatewaysAdapterConfiguration implements DataSourceConfiguration {
    private static final String CONFIG_PATH = "aiida.datasources.sga";
    private final Set<AiidaDataSource> enabledDataSources = new HashSet<>();

    public SmartGatewaysAdapterConfiguration(Environment environment) {
        var binder = Binder.get(environment);
        var bindResult = binder.bind(CONFIG_PATH, Bindable.setOf(SmartGatewaysAdapterDatasource.class));
        if (bindResult.isBound()) {
            instantiateSmartGatewaysAdapterFromConfig(bindResult.get());
        }
    }

    private void instantiateSmartGatewaysAdapterFromConfig(Set<SmartGatewaysAdapterDatasource> configs) {
        var enabledConfigs = configs.stream()
                                    .filter(SmartGatewaysAdapterDatasource::enabled)
                                    .toList();
        enabledConfigs.forEach(config -> enabledDataSources.add(
                new SmartGatewaysAdapter(
                        config.id(),
                        new MqttConfig.MqttConfigBuilder(
                                config.mqttServerUri(),
                                config.mqttSubscribeTopic()
                        ).setUsername(config.mqttUsername())
                         .setPassword(config.mqttPassword())
                         .build()
                )
        ));
    }

    @Override
    public Set<AiidaDataSource> enabledDataSources() {
        return enabledDataSources;
    }
}
