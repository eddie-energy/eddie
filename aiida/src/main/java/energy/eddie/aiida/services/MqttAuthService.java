package energy.eddie.aiida.services;

import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.errors.MqttUnauthorizedException;
import energy.eddie.aiida.models.datasource.MqttAction;
import energy.eddie.aiida.models.datasource.MqttDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MqttAuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttAuthService.class);
    private static final String SYS_BROKER_NODE_TOPIC_PREFIX = "$SYS/brokers/";

    private final MqttConfiguration mqttConfiguration;
    private final DataSourceService dataSourceService;

    public MqttAuthService(MqttConfiguration mqttConfiguration, DataSourceService dataSourceService) {
        this.mqttConfiguration = mqttConfiguration;
        this.dataSourceService = dataSourceService;
    }

    public void isAuthenticatedOrThrow(String username, String password) throws MqttUnauthorizedException {
        LOGGER.debug("Authenticating user");
        if (isAdmin(username, password)) {
            return;
        }

        getDataSourceForUsernameAndPassword(username, password);
    }

    public void isAdminOrThrow(String username, String password) throws MqttUnauthorizedException {
        LOGGER.debug("Checking admin user");
        if (!isAdmin(username, password)) {
            throw new MqttUnauthorizedException("User is not admin");
        }
    }

    public void isAuthorizedOrThrow(
            String username,
            String password,
            MqttAction mqttAction,
            String topic
    ) throws MqttUnauthorizedException {
        LOGGER.debug("Checking authorized user for action {}", mqttAction);
        if (isAdmin(username, password)) {
            return;
        }

        var dataSource = getDataSourceForUsernameAndPassword(username, password);
        if(!isAuthorizedForTopic(dataSource, topic)) {
            throw new MqttUnauthorizedException("User not authorized for topic");
        }
    }

    private boolean isAdmin(String username, String password) {
        return username.equals(mqttConfiguration.adminUsername()) && password.equals(mqttConfiguration.adminPassword());
    }

    private boolean isAuthorizedForTopic(MqttDataSource dataSource, String topic) {
        return dataSource.mqttSubscribeTopic().equals(topic) || topic.startsWith(SYS_BROKER_NODE_TOPIC_PREFIX);
    }

    private MqttDataSource getDataSourceForUsernameAndPassword(
            String username,
            String password
    ) throws MqttUnauthorizedException {
        return dataSourceService.findDataSourceAdapter(adapter -> {
                                    var dataSource = adapter.dataSource();
                                    return dataSource instanceof MqttDataSource mqttDataSource &&
                                           dataSource.enabled() &&
                                           mqttDataSource.mqttUsername().equals(username) &&
                                           mqttDataSource.mqttPassword().equals(password);
                                })
                                .map(adapter -> (MqttDataSource) adapter.dataSource())
                                .orElseThrow(() -> new MqttUnauthorizedException("User not found"));
    }
}
