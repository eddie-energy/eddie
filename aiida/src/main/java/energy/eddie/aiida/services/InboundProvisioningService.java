// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.adapters.datasource.inbound.InboundAdapter;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.dtos.datasource.mqtt.inbound.ProvisioningTypePatchDto;
import energy.eddie.aiida.dtos.inbound.ProvisioningConnectionDto;
import energy.eddie.aiida.errors.datasource.InvalidDataSourceTypeException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.repositories.PermissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class InboundProvisioningService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InboundProvisioningService.class);

    private final PermissionRepository permissionRepository;
    private final DataSourceService dataSourceService;
    private final MqttConfiguration mqttConfiguration;
    private final BCryptPasswordEncoder encoder;

    public InboundProvisioningService(
            PermissionRepository permissionRepository,
            DataSourceService dataSourceService,
            MqttConfiguration mqttConfiguration,
            BCryptPasswordEncoder encoder
    ) {
        this.permissionRepository = permissionRepository;
        this.dataSourceService = dataSourceService;
        this.mqttConfiguration = mqttConfiguration;
        this.encoder = encoder;
    }

    public ProvisioningConnectionDto changeInboundProvisioningType(
            ProvisioningTypePatchDto patchDto,
            UUID permissionId
    )
            throws PermissionNotFoundException, InvalidDataSourceTypeException {
        var dataSource = dataSource(permissionId);
        dataSource.changeInboundProvisioningType(patchDto.type());

        return dataSourceService.findDataSourceAdapter(dataSource.id())
                                .filter(InboundAdapter.class::isInstance)
                                .map(InboundAdapter.class::cast)
                                .map(adapter -> switchProvisioningType(patchDto, adapter))
                                .orElseGet(() -> {
                                    LOGGER.error("No datasource for permission {} was found", permissionId);
                                    return ProvisioningConnectionDto.empty();
                                });
    }

    private InboundDataSource dataSource(UUID permissionId)
            throws PermissionNotFoundException, InvalidDataSourceTypeException {
        var permission = permissionRepository.findById(permissionId)
                                             .orElseThrow(() -> new PermissionNotFoundException(permissionId));

        var dataSource = permission.dataSource();
        if (!(dataSource instanceof InboundDataSource inboundDataSource)) {
            throw new InvalidDataSourceTypeException();
        }

        return inboundDataSource;
    }

    private ProvisioningConnectionDto switchProvisioningType(
            ProvisioningTypePatchDto patchDto,
            InboundAdapter inboundAdapter
    ) {
        return switch (patchDto.type()) {
            case MQTT_CLIENT -> inboundAdapter.activateMqttClientProvisioningMode(
                    patchDto.host(),
                    patchDto.username(),
                    patchDto.password(),
                    patchDto.topic()
            );
            case MQTT_SERVER -> inboundAdapter.activateMqttServerProvisioningMode(
                    mqttConfiguration,
                    encoder
            );
            case REST_API_TOKEN, REST_BEARER -> {
                inboundAdapter.activateRestMode(patchDto.type());
                yield ProvisioningConnectionDto.empty();
            }
        };
    }
}
