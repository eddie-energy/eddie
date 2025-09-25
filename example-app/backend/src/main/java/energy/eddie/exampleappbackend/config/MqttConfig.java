package energy.eddie.exampleappbackend.config;

import energy.eddie.exampleappbackend.mqtt.MqttSubscriber;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Configuration
@AllArgsConstructor
@Slf4j
public class MqttConfig {
    private final ExampleAppMqttConfig mqttConfig;

    @Bean
    public MqttConnectionOptions getMqttConnectionOptions() {
        var options = new MqttConnectionOptions();
        options.setCleanStart(false);
        options.setAutomaticReconnect(true);
        options.setAutomaticReconnectDelay(30, 60 * 5);
        options.setUserName(mqttConfig.username());
        options.setPassword(mqttConfig.password().getBytes(StandardCharsets.UTF_8));
        return options;
    }

    @Bean
    public MqttClient mqttClient(ExampleAppMqttConfig config, MqttConnectionOptions options, MqttSubscriber mqttSubscriber) throws MqttException {
        var client = new MqttClient(config.serverUri(), config.clientId(), new MemoryPersistence());
        client.setCallback(mqttSubscriber);
        client.connect(options);
        client.subscribe(config.rtdTopic(), 0);
        log.info("Will subscribe to topic {}", config.rtdTopic());

        return client;
    }

}
