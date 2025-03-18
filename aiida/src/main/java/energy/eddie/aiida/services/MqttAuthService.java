package energy.eddie.aiida.services;

import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.models.datasource.MqttAction;
import energy.eddie.aiida.models.datasource.MqttDataSource;
import energy.eddie.aiida.repositories.MqttDataSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MqttAuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttAuthService.class);
    private static final String CLUSTER_NODE_LIST_TOPIC_PREFIX = "$SYS/brokers/";

    private final MqttConfiguration mqttConfiguration;
    private final MqttDataSourceRepository mqttDataSourceRepository;

    public MqttAuthService(MqttConfiguration mqttConfiguration, MqttDataSourceRepository mqttDataSourceRepository) {
        this.mqttConfiguration = mqttConfiguration;
        this.mqttDataSourceRepository = mqttDataSourceRepository;
    }

    public boolean authenticate(String username, String password) {
        LOGGER.debug("Authenticating user {}", username);
        if(isAdmin(username, password)) {
            return true;
        }

        var dataSource = getDataSourceForUsernameAndPassword(username, password);
        return dataSource != null;
    }

    public boolean isAdmin(String username, String password) {
        LOGGER.debug("Checking admin user {}", username);
        return username.equals(mqttConfiguration.adminUsername()) && password.equals(mqttConfiguration.adminPassword());
    }

    public boolean isAuthorized(String username, String password, MqttAction mqttAction, String topic) {
        LOGGER.debug("Checking authorized user {}, action {}, topic {}", username, mqttAction, topic);
        if(isAdmin(username, password)) {
            return true;
        }

        var dataSource = getDataSourceForUsernameAndPassword(username, password);
        return dataSource != null && isAuthorizedForTopic(dataSource, topic);
    }

    private boolean isAuthorizedForTopic(MqttDataSource dataSource, String topic) {
        return dataSource.mqttSubscribeTopic().equals(topic) || topic.startsWith(CLUSTER_NODE_LIST_TOPIC_PREFIX);
    }

    private MqttDataSource getDataSourceForUsernameAndPassword(String username, String password) {
        return mqttDataSourceRepository.findByMqttUsernameAndMqttPassword(username, password);
    }
}
