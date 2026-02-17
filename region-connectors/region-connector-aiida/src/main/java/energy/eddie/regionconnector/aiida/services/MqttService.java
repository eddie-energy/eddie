// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.services;

import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.api.agnostic.aiida.mqtt.MqttDto;
import energy.eddie.regionconnector.aiida.config.AiidaConfiguration;
import energy.eddie.regionconnector.aiida.exceptions.CredentialsAlreadyExistException;
import energy.eddie.regionconnector.aiida.mqtt.acl.MqttAclRepository;
import energy.eddie.regionconnector.aiida.mqtt.callback.MqttMessageCallback;
import energy.eddie.regionconnector.aiida.mqtt.topic.MqttTopic;
import energy.eddie.regionconnector.aiida.mqtt.topic.MqttTopicType;
import energy.eddie.regionconnector.aiida.mqtt.user.MqttUser;
import energy.eddie.regionconnector.aiida.mqtt.user.MqttUserRepository;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import energy.eddie.regionconnector.shared.utils.PasswordGenerator;
import jakarta.transaction.Transactional;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class MqttService implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttService.class);
    private static final int PASSWORD_LENGTH = 24;
    private final MqttUserRepository userRepository;
    private final MqttAclRepository aclRepository;
    private final PasswordGenerator passwordGenerator;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final MqttAsyncClient mqttClient;
    private final AiidaConfiguration aiidaConfiguration;
    private final ObjectMapper objectMapper;

    public MqttService(
            MqttUserRepository userRepository,
            MqttAclRepository aclRepository,
            PasswordGenerator passwordGenerator,
            BCryptPasswordEncoder bCryptPasswordEncoder,
            MqttAsyncClient mqttClient,
            AiidaConfiguration aiidaConfiguration,
            MqttMessageCallback mqttMessageCallback,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.aclRepository = aclRepository;
        this.passwordGenerator = passwordGenerator;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.mqttClient = mqttClient;
        this.mqttClient.setCallback(mqttMessageCallback);
        this.aiidaConfiguration = aiidaConfiguration;
        this.objectMapper = objectMapper;
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
    public MqttDto createCredentialsAndAclForPermission(
            String permissionId,
            boolean isInbound
    ) throws CredentialsAlreadyExistException {
        LOGGER.info("Creating MQTT credentials and ACLs for permission {}", permissionId);

        if (userRepository.existsByUsername(permissionId)) {
            throw new CredentialsAlreadyExistException(permissionId);
        }

        var wrapper = createAndSaveMqttUser(permissionId);
        var topics = createAclsForUser(wrapper.user, isInbound);

        return new MqttDto(aiidaConfiguration.mqttServerUri(),
                           wrapper.user().username(),
                           wrapper.rawPassword(),
                           topics.dataTopic().aiidaTopic(),
                           topics.statusTopic().aiidaTopic(),
                           topics.terminationTopic().aiidaTopic());
    }

    /**
     * Deletes all associated ACLs for the specified permission ID.
     *
     * @param permissionId The permission ID for which to delete the credentials and ACLs.
     */
    @Transactional
    public void deleteAclsForPermission(String permissionId) {
        LOGGER.info("Deleting MQTT ACLs for permission {}", permissionId);
        aclRepository.deleteByUsername(permissionId);
    }

    @Override
    public void close() throws MqttException {
        mqttClient.disconnect(3000);
        mqttClient.close(true);
    }

    public void subscribeToOutboundDataTopic(String permissionId) throws MqttException {
        var topic = MqttTopic.of(permissionId, MqttTopicType.OUTBOUND_DATA).eddieTopic();
        LOGGER.info("Subscribing to outbound data topic {}", topic);

        mqttClient.subscribe(topic, 1);
    }

    public void subscribeToStatusTopic(String permissionId) throws MqttException {
        var topic = MqttTopic.of(permissionId, MqttTopicType.STATUS).eddieTopic();
        LOGGER.info("Subscribing to status topic {}", topic);

        mqttClient.subscribe(topic, 1);
    }

    public void sendTerminationRequest(AiidaPermissionRequest permissionRequest) throws MqttException {
        mqttClient.publish(permissionRequest.terminationTopic(),
                           permissionRequest.permissionId().getBytes(StandardCharsets.UTF_8),
                           1,
                           true);
    }

    public <T> void publishInboundData(AiidaSchema schema, String permissionId, T payload) throws MqttException {
        var topic = MqttTopic.of(permissionId, MqttTopicType.INBOUND_DATA).schemaTopic(schema);
        LOGGER.info("Publishing inbound data to topic {}", topic);

        var payloadBytes = objectMapper.writeValueAsBytes(payload);

        mqttClient.publish(topic, payloadBytes, 1, false);
    }

    /**
     * Creates a new {@link MqttUser} with a random password and saves it to the database.
     */
    @SuppressWarnings("NullAway")
    // bCryptPasswordEncoder.encode(rawPassword) will only return null if rawPassword is null, which it never will be.
    private UserPasswordWrapper createAndSaveMqttUser(String permissionId) {
        String rawPassword = passwordGenerator.generatePassword(PASSWORD_LENGTH);
        // BCryptPasswordEncoder will generate and store the salt in the hash
        var passwordHash = bCryptPasswordEncoder.encode(rawPassword);

        var mqttUser = new MqttUser(permissionId, passwordHash, false);
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
    private Topics createAclsForUser(MqttUser mqttUser, boolean isInbound) {
        var dataTopicType = isInbound ? MqttTopicType.INBOUND_DATA : MqttTopicType.OUTBOUND_DATA;
        var topics = new Topics(MqttTopic.of(mqttUser.permissionId(), dataTopicType),
                                MqttTopic.of(mqttUser.permissionId(), MqttTopicType.STATUS),
                                MqttTopic.of(mqttUser.permissionId(), MqttTopicType.TERMINATION));

        aclRepository.saveAll(List.of(
                topics.dataTopic().aiidaAcl(mqttUser.username()),
                topics.statusTopic().aiidaAcl(mqttUser.username()),
                topics.terminationTopic().aiidaAcl(mqttUser.username())));

        return topics;
    }

    private record UserPasswordWrapper(MqttUser user, String rawPassword) {}

    private record Topics(MqttTopic dataTopic, MqttTopic statusTopic, MqttTopic terminationTopic) {}
}
