package energy.eddie.aiida.services;

import energy.eddie.aiida.config.MQTTConfiguration;
import energy.eddie.aiida.models.datasource.MqttAction;
import energy.eddie.aiida.models.datasource.MqttDataSource;
import energy.eddie.aiida.repositories.MqttDataSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MqttAuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttAuthService.class);

    private final MQTTConfiguration mqttConfiguration;
    private final MqttDataSourceRepository mqttDataSourceRepository;

    public MqttAuthService(MQTTConfiguration mQTTConfiguration, MqttDataSourceRepository mqttDataSourceRepository) {
        this.mqttConfiguration = mQTTConfiguration;
        this.mqttDataSourceRepository = mqttDataSourceRepository;
    }

    public boolean authenticate(String username, String password) {
        LOGGER.info("Authenticating user {}", username);

        var dataSource = getDataSourceForUsernameAndPassword(username, password);
        return dataSource != null;
    }

    public boolean isAdmin(String username, String password) {
        LOGGER.info("Checking admin user {}", username);
        return username.equals(mqttConfiguration.adminUsername()) && password.equals(mqttConfiguration.adminPassword());
    }

    public boolean isAuthorized(String username, String password, MqttAction mqttAction, String topic) {
        LOGGER.info("Checking authorized user {}, action {}, topic {}", username, mqttAction, topic);
        if(isAdmin(username, password)) {
            return true;
        }

        var dataSource = getDataSourceForUsernameAndPassword(username, password);
        return dataSource != null && dataSource.getMqttSubscribeTopic().equals(topic);
    }

    private MqttDataSource getDataSourceForUsernameAndPassword(String username, String password) {
        return mqttDataSourceRepository.findByMqttUsernameAndMqttPassword(username, password);
    }
}
