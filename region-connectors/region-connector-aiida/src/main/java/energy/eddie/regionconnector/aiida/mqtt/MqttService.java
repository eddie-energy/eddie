package energy.eddie.regionconnector.aiida.mqtt;

import energy.eddie.regionconnector.aiida.exceptions.CredentialsAlreadyExistException;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import energy.eddie.regionconnector.shared.utils.PasswordGenerator;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class MqttService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttService.class);
    private static final int PASSWORD_LENGTH = 24;
    private static final String AIIDA_TOPIC_NAME_PREFIX = "aiida/v1";
    private final MqttUserRepository userRepository;
    private final MqttAclRepository aclRepository;
    private final PasswordGenerator passwordGenerator;
    private final BCryptPasswordEncoder encoder;
    private final MqttAsyncClient mqttClient;

    public MqttService(
            MqttUserRepository userRepository,
            MqttAclRepository aclRepository,
            PasswordGenerator passwordGenerator,
            BCryptPasswordEncoder encoder,
            MqttAsyncClient mqttClient
    ) {
        this.userRepository = userRepository;
        this.aclRepository = aclRepository;
        this.passwordGenerator = passwordGenerator;
        this.encoder = encoder;
        this.mqttClient = mqttClient;
        this.mqttClient.setCallback(new LoggingMqttCallback());
    }

    /**
     * Creates and saves a new {@link MqttUser} with a random password for the specified permission and creates ACLs to
     * allow the new user to publish data and status messages, as well as to subscribe to its termination topic. The
     * newly created user and ACLs will be available to the MQTT broker, if it's configured to read from the database.
     * <p>
     * <b>Important:</b> This method does not validate if there is a permission with the ID {@code permissionId}.
     * </p>
     *
     * @param permissionId For which permission to create the user.
     * @return MqttDto that contains the topics and the {@link MqttUser} with its password.
     * @throws IllegalArgumentException If there is already a MqttUser for the permissionId.
     */
    public MqttDto createCredentialsAndAclForPermission(String permissionId) throws CredentialsAlreadyExistException {
        LOGGER.info("Creating MQTT credentials and ACLs for permission {}", permissionId);

        if (userRepository.existsByPermissionId(permissionId))
            throw new CredentialsAlreadyExistException(permissionId);

        var wrapper = createAndSaveMqttUser(permissionId);
        var topics = createAclsForUser(wrapper.user);

        return new MqttDto(wrapper.user().username(),
                           wrapper.rawPassword(),
                           topics.publishTopic(),
                           topics.statusMessageTopic(),
                           topics.terminationTopic());
    }

    /**
     * Creates a new {@link MqttUser} with a random password and saves it to the database.
     */
    private UserPasswordWrapper createAndSaveMqttUser(String permissionId) {
        String rawPassword = passwordGenerator.generatePassword(PASSWORD_LENGTH);
        // BCryptPasswordEncoder will generate and store the salt in the hash
        var passwordHash = encoder.encode(rawPassword);

        MqttUser mqttUser = new MqttUser(permissionId, passwordHash, false, permissionId);
        return new UserPasswordWrapper(userRepository.save(mqttUser), rawPassword);
    }

    /**
     * Creates the following ACLs for the supplied {@link MqttUser}:
     * <ul>
     *     <li>data topic: publish</li>
     *     <li>status message topic: publish</li>
     *     <li>termination topic: subscribe</li>
     * </ul>
     * No other ACLs are defined, make sure to properly configure your MQTT server with a deny-all for unmatched topics.
     */
    private Topics createAclsForUser(MqttUser mqttUser) {
        var topics = new Topics(getTopicForPermission(mqttUser.permissionId(), TopicType.DATA),
                                getTopicForPermission(mqttUser.permissionId(), TopicType.STATUS),
                                getTopicForPermission(mqttUser.permissionId(), TopicType.TERMINATION));

        var dataAcl = new MqttAcl(mqttUser.username(),
                                  MqttAction.PUBLISH,
                                  MqttAclType.ALLOW,
                                  topics.publishTopic());

        var statusAcl = new MqttAcl(mqttUser.username(),
                                    MqttAction.PUBLISH,
                                    MqttAclType.ALLOW,
                                    topics.statusMessageTopic());

        var terminationAcl = new MqttAcl(mqttUser.username(),
                                         MqttAction.SUBSCRIBE,
                                         MqttAclType.ALLOW,
                                         topics.terminationTopic());


        aclRepository.saveAll(List.of(dataAcl, statusAcl, terminationAcl));
        return topics;
    }

    private static String getTopicForPermission(String permissionId, TopicType topicType) {
        return AIIDA_TOPIC_NAME_PREFIX + "/" + permissionId + "/" + topicType.topicName();
    }

    private enum TopicType {
        DATA("data"),
        STATUS("status"),
        TERMINATION("termination");

        private final String topicName;

        TopicType(String topicName) {
            this.topicName = topicName;
        }

        public String topicName() {
            return topicName;
        }
    }

    private record UserPasswordWrapper(MqttUser user, String rawPassword) {}

    private record Topics(String publishTopic, String statusMessageTopic, String terminationTopic) {}

    public void sendTerminationRequest(AiidaPermissionRequest permissionRequest) throws MqttException {
        mqttClient.publish(permissionRequest.terminationTopic(),
                           permissionRequest.permissionId().getBytes(StandardCharsets.UTF_8),
                           1,
                           true);
    }

    private static class LoggingMqttCallback implements MqttCallback {
        @Override
        public void disconnected(MqttDisconnectResponse disconnectResponse) {
            LOGGER.warn("Disconnected from MQTT broker {}", disconnectResponse);
        }

        @Override
        public void mqttErrorOccurred(MqttException exception) {
            LOGGER.error("Mqtt error occurred", exception);
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            // Not needed, as no messages are read from the broker
        }

        @Override
        public void deliveryComplete(IMqttToken token) {
            LOGGER.trace("Delivery complete for MqttToken {}", token);
        }

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            LOGGER.info("Connected to MQTT broker {}, was because of reconnect: {}", serverURI, reconnect);
        }

        @Override
        public void authPacketArrived(int reasonCode, MqttProperties properties) {
            // Not needed, as no advanced authentication is required
        }
    }
}
