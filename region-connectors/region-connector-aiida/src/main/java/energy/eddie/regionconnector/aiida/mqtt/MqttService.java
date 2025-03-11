package energy.eddie.regionconnector.aiida.mqtt;

import energy.eddie.api.agnostic.aiida.MqttDto;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.aiida.InboundAiidaDataNeed;
import energy.eddie.regionconnector.aiida.config.AiidaConfiguration;
import energy.eddie.regionconnector.aiida.exceptions.CredentialsAlreadyExistException;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import energy.eddie.regionconnector.shared.utils.PasswordGenerator;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class MqttService implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttService.class);
    private static final int PASSWORD_LENGTH = 24;
    private static final String AIIDA_TOPIC_NAME_PREFIX = "aiida/v1";
    private final MqttUserRepository userRepository;
    private final MqttAclRepository aclRepository;
    private final PasswordGenerator passwordGenerator;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final MqttAsyncClient mqttClient;
    private final AiidaConfiguration aiidaConfiguration;

    public MqttService(
            MqttUserRepository userRepository,
            MqttAclRepository aclRepository,
            PasswordGenerator passwordGenerator,
            BCryptPasswordEncoder bCryptPasswordEncoder,
            MqttAsyncClient mqttClient,
            AiidaConfiguration aiidaConfiguration,
            MqttMessageCallback mqttMessageCallback
    ) {
        this.userRepository = userRepository;
        this.aclRepository = aclRepository;
        this.passwordGenerator = passwordGenerator;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.mqttClient = mqttClient;
        this.mqttClient.setCallback(mqttMessageCallback);
        this.aiidaConfiguration = aiidaConfiguration;
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
     * @param dataNeed     The data need associated with the permission.
     * @return MqttDto that contains the topics and the {@link MqttUser} with its password.
     * @throws IllegalArgumentException If there is already a MqttUser for the permissionId.
     */
    public MqttDto createCredentialsAndAclForPermission(
            String permissionId,
            DataNeed dataNeed
    ) throws CredentialsAlreadyExistException {
        LOGGER.info("Creating MQTT credentials and ACLs for permission {}", permissionId);

        if (userRepository.existsByPermissionId(permissionId))
            throw new CredentialsAlreadyExistException(permissionId);

        var wrapper = createAndSaveMqttUser(permissionId);
        var topics = createAclsForUser(wrapper.user, dataNeed);

        return new MqttDto(aiidaConfiguration.mqttServerUri(),
                           wrapper.user().username(),
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
        var passwordHash = bCryptPasswordEncoder.encode(rawPassword);

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
    private Topics createAclsForUser(MqttUser mqttUser, DataNeed dataNeed) {
        var topics = new Topics(getTopicForPermission(mqttUser.permissionId(), TopicType.DATA),
                                getTopicForPermission(mqttUser.permissionId(), TopicType.STATUS),
                                getTopicForPermission(mqttUser.permissionId(), TopicType.TERMINATION));

        var mqttAction = dataNeed instanceof InboundAiidaDataNeed ? MqttAction.SUBSCRIBE : MqttAction.PUBLISH;

        var dataAcl = new MqttAcl(mqttUser.username(),
                                  mqttAction,
                                  MqttAclType.ALLOW,
                                  topics.publishTopic());

        var statusAcl = new MqttAcl(mqttUser.username(),
                                    mqttAction,
                                    MqttAclType.ALLOW,
                                    topics.statusMessageTopic());

        var terminationAcl = new MqttAcl(mqttUser.username(),
                                         mqttAction.getComplementaryAction(),
                                         MqttAclType.ALLOW,
                                         topics.terminationTopic());


        aclRepository.saveAll(List.of(dataAcl, statusAcl, terminationAcl));
        return topics;
    }

    private static String getTopicForPermission(String permissionId, TopicType topicType) {
        return AIIDA_TOPIC_NAME_PREFIX + "/" + permissionId + "/" + topicType.topicName();
    }

    @Override
    public void close() throws MqttException {
        mqttClient.disconnect(3000);
        mqttClient.close(true);
    }

    private record UserPasswordWrapper(MqttUser user, String rawPassword) {}

    private record Topics(String publishTopic, String statusMessageTopic, String terminationTopic) {}

    public void subscribeToStatusTopic(String permissionId) throws MqttException {
        mqttClient.subscribe(getTopicForPermission(permissionId, TopicType.STATUS), 1);
    }

    public void sendTerminationRequest(AiidaPermissionRequest permissionRequest) throws MqttException {
        mqttClient.publish(permissionRequest.terminationTopic(),
                           permissionRequest.permissionId().getBytes(StandardCharsets.UTF_8),
                           1,
                           true);
    }
}
