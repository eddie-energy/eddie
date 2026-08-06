// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.aggregator.InboundAggregator;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.dtos.provisioning.MqttProvisioningConnectionDto;
import energy.eddie.aiida.dtos.provisioning.ProvisioningTypePatchDto;
import energy.eddie.aiida.errors.datasource.InvalidDataSourceTypeException;
import energy.eddie.aiida.errors.inbound.ProvisioningConfigurationException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.models.datasource.mqtt.SecretGenerator;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundProvisioningType;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.provisioning.ProvisioningMqttPublisher;
import energy.eddie.aiida.repositories.InboundDataSourceRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProvisioningService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ProvisioningService.class);
    private static final Set<InboundProvisioningType> MQTT_PROVISIONING_TYPES =
            Set.of(
                    InboundProvisioningType.MQTT_CLIENT,
                    InboundProvisioningType.MQTT_SERVER
            );

    private final InboundDataSourceRepository inboundDataSourceRepository;
    private final InboundAggregator inboundAggregator;
    private final PermissionRepository permissionRepository;
    private final MqttConfiguration mqttConfiguration;
    private final BCryptPasswordEncoder passwordEncoder;

    private final Map<UUID, ProvisioningMqttPublisher> mqttPublishers =
            new ConcurrentHashMap<>();

    @Nullable
    private Disposable inboundRecordSubscription;

    public ProvisioningService(
            InboundAggregator inboundAggregator,
            PermissionRepository permissionRepository,
            MqttConfiguration mqttConfiguration,
            BCryptPasswordEncoder passwordEncoder,
            InboundDataSourceRepository inboundDataSourceRepository
    ) {
        this.inboundAggregator = inboundAggregator;
        this.permissionRepository = permissionRepository;
        this.mqttConfiguration = mqttConfiguration;
        this.passwordEncoder = passwordEncoder;
        this.inboundDataSourceRepository = inboundDataSourceRepository;
    }

    /**
     * Changes the provisioning type of the inbound data source associated with a permission. MQTT modes establish a
     * connection and replace the active publisher, while REST modes stop any active publisher and clear the MQTT
     * provisioning configuration.
     *
     * @param permissionId ID of the permission whose inbound data source should be reconfigured.
     * @param patch        Requested provisioning type and, for MQTT client mode, its connection parameters.
     * @return Connection details for MQTT provisioning, or an empty DTO for REST provisioning.
     */
    @Transactional(rollbackOn = ProvisioningConfigurationException.class)
    public MqttProvisioningConnectionDto changeProvisioningType(
            UUID permissionId,
            ProvisioningTypePatchDto patch
    ) throws PermissionNotFoundException,
             InvalidDataSourceTypeException,
             ProvisioningConfigurationException {
        var dataSource = inboundDataSource(permissionId);

        return switch (patch.type()) {
            case MQTT_CLIENT -> activateMqttClient(dataSource, patch);
            case MQTT_SERVER -> activateMqttServer(dataSource);
            case REST_API_TOKEN, REST_BEARER -> {
                stopPublisher(dataSource.id());
                dataSource.changeInboundProvisioningType(patch.type());
                yield MqttProvisioningConnectionDto.empty();
            }
        };
    }

    /**
     * Starts forwarding inbound records and restores publishers for existing MQTT provisioning configurations.
     */
    @PostConstruct
    void initialize() {
        subscribeToInboundRecords();
        restartPersistedMqttPublishers();
    }

    /**
     * Subscribes once to the inbound record stream and forwards records asynchronously to their provisioning publisher.
     */
    void subscribeToInboundRecords() {
        inboundRecordSubscription = inboundAggregator.inboundRecordFlux()
                                                     .publishOn(Schedulers.boundedElastic())
                                                     .subscribe(
                                                             this::publishProvisionRecord,
                                                             error -> LOGGER.error("Inbound provisioning stream failed",
                                                                                   error)
                                                     );
    }

    @PreDestroy
    void close() {
        if (inboundRecordSubscription != null) {
            inboundRecordSubscription.dispose();
        }

        mqttPublishers.values().forEach(ProvisioningMqttPublisher::close);
        mqttPublishers.clear();
    }

    private MqttProvisioningConnectionDto activateMqttClient(
            InboundDataSource dataSource,
            ProvisioningTypePatchDto patch
    ) throws ProvisioningConfigurationException {
        var response = dataSource.establishClientModeConnection(
                patch.host(),
                patch.username(),
                patch.password(),
                patch.topic()
        );

        replacePublisher(dataSource);
        return response;
    }

    private MqttProvisioningConnectionDto activateMqttServer(InboundDataSource dataSource)
            throws ProvisioningConfigurationException {
        var plaintextPassword = SecretGenerator.generate();

        var response = dataSource.establishServerModeConnection(
                mqttConfiguration,
                passwordEncoder,
                plaintextPassword
        );

        replacePublisher(dataSource);
        return response;
    }

    /**
     * Stops and removes the MQTT publisher for an inbound data source.
     *
     * @param dataSourceId ID of the inbound data source whose publisher should be stopped.
     */
    void stopPublisher(UUID dataSourceId) {
        var publisher = mqttPublishers.remove(dataSourceId);
        if (publisher != null) {
            publisher.close();
        }
    }

    /**
     * Recreates publishers for client- and server-mode provisioning after application startup.
     */
    private void restartPersistedMqttPublishers() {
        var dataSources =
                inboundDataSourceRepository.findByProvisioningTypeIn(
                        MQTT_PROVISIONING_TYPES
                );

        for (var dataSource : dataSources) {
            try {
                replacePublisher(dataSource);
                LOGGER.info(
                        "Restarted MQTT provisioning publisher for data source {}",
                        dataSource.id()
                );
            } catch (ProvisioningConfigurationException | RuntimeException exception) {
                LOGGER.error(
                        "Could not restart MQTT provisioning publisher for data source {}",
                        dataSource.id(),
                        exception
                );
            }
        }
    }

    private void publishProvisionRecord(InboundRecord inboundRecord) {
        var dataSource = inboundRecord.dataSource();
        var type = dataSource.inboundProvisioningType();

        if (type != InboundProvisioningType.MQTT_CLIENT &&
            type != InboundProvisioningType.MQTT_SERVER) {
            return;
        }

        var publisher = mqttPublishers.get(dataSource.id());
        if (publisher == null) {
            LOGGER.error(
                    "No MQTT provisioning publisher exists for data source {}",
                    dataSource.id()
            );
            return;
        }

        publisher.publish(inboundRecord);
    }

    /**
     * Creates and registers a publisher for the current provisioning mode, closing any publisher it replaces. Client
     * mode authenticates with its persisted external credentials; server mode uses AIIDA's broker identity because the
     * persisted per-permission password is a one-way hash intended for broker authentication.
     *
     * @param dataSource Inbound data source whose publisher should be created or replaced.
     */
    private void replacePublisher(InboundDataSource dataSource) throws ProvisioningConfigurationException {
        var connection = dataSource.provisioningConnection();
        var topic = dataSource.provisioningTopic();
        var publisher = dataSource.inboundProvisioningType() == InboundProvisioningType.MQTT_SERVER
                ? new ProvisioningMqttPublisher(
                connection,
                topic,
                mqttConfiguration.username(),
                mqttConfiguration.password()
        )
                : new ProvisioningMqttPublisher(connection, topic);

        var previous = mqttPublishers.put(dataSource.id(), publisher);
        if (previous != null) {
            previous.close();
        }
    }

    private InboundDataSource inboundDataSource(UUID permissionId)
            throws PermissionNotFoundException, InvalidDataSourceTypeException {
        var permission = permissionRepository.findById(permissionId)
                                             .orElseThrow(() -> new PermissionNotFoundException(permissionId));

        if (permission.dataSource() instanceof InboundDataSource dataSource) {
            return dataSource;
        }

        throw new InvalidDataSourceTypeException();
    }
}
