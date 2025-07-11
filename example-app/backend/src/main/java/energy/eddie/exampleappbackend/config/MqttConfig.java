package energy.eddie.exampleappbackend.config;

import lombok.AllArgsConstructor;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Configuration
@AllArgsConstructor
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
}
