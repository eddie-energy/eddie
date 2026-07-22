// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.aggregator.InboundAggregator;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.dtos.provisioning.ProvisioningConnectionDto;
import energy.eddie.aiida.dtos.provisioning.ProvisioningTypePatchDto;
import energy.eddie.aiida.errors.datasource.InvalidDataSourceTypeException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.models.datasource.mqtt.SecretGenerator;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundProvisioningType;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.provisioning.ProvisioningMqttPublisher;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProvisioningService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ProvisioningService.class);

    private final InboundAggregator inboundAggregator;
    private final PermissionRepository permissionRepository;
    private final MqttConfiguration mqttConfiguration;
    private final BCryptPasswordEncoder passwordEncoder;

    private final Map<UUID, ProvisioningMqttPublisher> mqttPublishers =
            new ConcurrentHashMap<>();

    @Nullable
    private Disposable inboundRecordSubscription;

    /**
     * Creates a service that manages provisioning configuration and MQTT publishers for inbound data sources.
     *
     * @param inboundAggregator    Aggregator whose inbound records are forwarded to active provisioning publishers.
     * @param permissionRepository Repository used to resolve the permission and its inbound data source.
     * @param mqttConfiguration    MQTT broker configuration used for server-mode provisioning.
     * @param passwordEncoder      Encoder used to store server-mode MQTT credentials securely.
     */
    public ProvisioningService(
            InboundAggregator inboundAggregator,
            PermissionRepository permissionRepository,
            MqttConfiguration mqttConfiguration,
            BCryptPasswordEncoder passwordEncoder
    ) {
        this.inboundAggregator = inboundAggregator;
        this.permissionRepository = permissionRepository;
        this.mqttConfiguration = mqttConfiguration;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Changes the provisioning type of the inbound data source associated with a permission. MQTT modes establish a
     * connection and replace the active publisher, while REST modes stop any active publisher and clear the MQTT
     * provisioning configuration.
     *
     * @param permissionId ID of the permission whose inbound data source should be reconfigured.
     * @param patch        Requested provisioning type and, for MQTT client mode, its connection parameters.
     * @return Connection details for MQTT provisioning, or an empty DTO for REST provisioning.
     * @throws PermissionNotFoundException    If no permission exists for {@code permissionId}.
     * @throws InvalidDataSourceTypeException If the permission is not associated with an inbound data source.
     */
    @Transactional
    public ProvisioningConnectionDto changeProvisioningType(
            UUID permissionId,
            ProvisioningTypePatchDto patch
    ) throws PermissionNotFoundException, InvalidDataSourceTypeException {
        var dataSource = inboundDataSource(permissionId);

        return switch (patch.type()) {
            case MQTT_CLIENT -> activateMqttClient(dataSource, patch);
            case MQTT_SERVER -> activateMqttServer(dataSource);
            case REST_API_TOKEN, REST_BEARER -> {
                stopPublisher(dataSource.id());
                dataSource.changeInboundProvisioningType(patch.type());
                yield ProvisioningConnectionDto.empty();
            }
        };
    }

    @PostConstruct
    void subscribeToInboundRecords() {
        inboundRecordSubscription = inboundAggregator.inboundRecordFlux()
                                                     .publishOn(Schedulers.boundedElastic())
                                                     .subscribe(
                                                             this::provisionRecord,
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

    private ProvisioningConnectionDto activateMqttClient(
            InboundDataSource dataSource,
            ProvisioningTypePatchDto patch
    ) {
        var response = dataSource.establishClientModeConnection(
                patch.host(),
                patch.username(),
                patch.password(),
                patch.topic()
        );

        replacePublisher(dataSource);
        return response;
    }

    private ProvisioningConnectionDto activateMqttServer(InboundDataSource dataSource) {
        var plaintextPassword = SecretGenerator.generate();

        var response = dataSource.establishServerModeConnection(
                mqttConfiguration,
                passwordEncoder,
                plaintextPassword
        );

        replacePublisher(dataSource);
        return response;
    }

    private void provisionRecord(InboundRecord inboundRecord) {
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

    private void replacePublisher(InboundDataSource dataSource) {
        var publisher = new ProvisioningMqttPublisher(
                dataSource.provisioningConnection(),
                dataSource.provisioningTopicOrThrow()
        );

        var previous = mqttPublishers.put(dataSource.id(), publisher);
        if (previous != null) {
            previous.close();
        }
    }

    private void stopPublisher(UUID dataSourceId) {
        var publisher = mqttPublishers.remove(dataSourceId);
        if (publisher != null) {
            publisher.close();
        }
    }

    private InboundDataSource inboundDataSource(UUID permissionId)
            throws PermissionNotFoundException, InvalidDataSourceTypeException {
        var permission = permissionRepository.findById(permissionId)
                                             .orElseThrow(() -> new PermissionNotFoundException(permissionId));

        if (!(permission.dataSource() instanceof InboundDataSource dataSource)) {
            throw new InvalidDataSourceTypeException();
        }

        return dataSource;
    }
}
