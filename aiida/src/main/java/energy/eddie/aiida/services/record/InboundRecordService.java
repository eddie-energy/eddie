// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.record;

import energy.eddie.aiida.dtos.record.InboundRecordDto;
import energy.eddie.aiida.errors.SecretLoadingException;
import energy.eddie.aiida.errors.auth.UnauthorizedException;
import energy.eddie.aiida.errors.inbound.ProvisioningTypeNotConfiguredException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.errors.record.InboundRecordNotFoundException;
import energy.eddie.aiida.errors.record.UnsupportedInboundRecordTransformationException;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundProvisioningType;
import energy.eddie.aiida.models.permission.InboundPermission;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.repositories.InboundPermissionRepository;
import energy.eddie.aiida.repositories.InboundRecordRepository;
import energy.eddie.aiida.services.record.transform.InboundPayloadTransformationService;
import energy.eddie.aiida.services.secrets.SecretsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
public class InboundRecordService {
    private static final Logger LOGGER = LoggerFactory.getLogger(InboundRecordService.class);

    private final InboundRecordRepository inboundRecordRepository;
    private final InboundPermissionRepository inboundPermissionRepository;
    private final InboundPayloadTransformationService inboundPayloadTransformationService;
    private final SecretsService secretsService;

    /**
     * Creates a service for retrieving, authorizing, and transforming inbound records.
     *
     * @param inboundRecordRepository             Repository containing inbound records.
     * @param inboundPermissionRepository         Repository used to resolve inbound permissions.
     * @param inboundPayloadTransformationService Service used to transform stored payloads into the requested format.
     * @param secretsService                      Service used to load the inbound access code.
     */
    public InboundRecordService(
            InboundRecordRepository inboundRecordRepository,
            InboundPermissionRepository inboundPermissionRepository,
            InboundPayloadTransformationService inboundPayloadTransformationService,
            SecretsService secretsService
    ) {
        this.inboundRecordRepository = inboundRecordRepository;
        this.inboundPermissionRepository = inboundPermissionRepository;
        this.inboundPayloadTransformationService = inboundPayloadTransformationService;
        this.secretsService = secretsService;
    }

    /**
     * Returns the latest inbound record after validating the access code and configured REST provisioning type.
     *
     * @param permissionId     ID of the inbound permission whose latest record should be returned.
     * @param accessCode       Access code supplied by the caller.
     * @param provisioningType REST provisioning type through which the caller supplied the access code.
     * @return The latest inbound record transformed to the permission's configured message format.
     * @throws PermissionNotFoundException                     If no inbound permission with the ID exists.
     * @throws UnauthorizedException                           If the supplied access code is invalid.
     * @throws InboundRecordNotFoundException                  If no inbound record exists for the data source.
     * @throws UnsupportedInboundRecordTransformationException If the record cannot be transformed to the configured format.
     * @throws ProvisioningTypeNotConfiguredException          If the requested REST provisioning type is not active.
     * @throws SecretLoadingException                          If the stored access code cannot be loaded.
     */
    public InboundRecordDto latestRecord(
            UUID permissionId,
            String accessCode,
            InboundProvisioningType provisioningType
    ) throws PermissionNotFoundException, UnauthorizedException,
             InboundRecordNotFoundException,
             UnsupportedInboundRecordTransformationException,
             ProvisioningTypeNotConfiguredException, SecretLoadingException {
        var permission = permission(permissionId);
        var dataSource = dataSource(permission);

        if (dataSource.inboundProvisioningType() != provisioningType) {
            throw new ProvisioningTypeNotConfiguredException(permissionId, dataSource.inboundProvisioningType());
        }

        var savedAccessCode = secretsService.loadSecret(dataSource.accessCode());
        if (!Objects.equals(savedAccessCode, accessCode)) {
            throw new UnauthorizedException(
                    "Access code does not match for data source with ID: " + dataSource.id()
            );
        }

        return toDto(permission, latestRecord(permissionId, dataSource));
    }

    /**
     * Returns the latest inbound record without performing access-code or provisioning-type validation.
     *
     * @param permissionId ID of the inbound permission whose latest record should be returned.
     * @return The latest inbound record transformed to the permission's configured message format.
     * @throws PermissionNotFoundException                     If no inbound permission with the ID exists.
     * @throws InboundRecordNotFoundException                  If no inbound record exists for the data source.
     * @throws UnsupportedInboundRecordTransformationException If the record cannot be transformed to the configured format.
     */
    public InboundRecordDto latestRecord(UUID permissionId)
            throws PermissionNotFoundException, InboundRecordNotFoundException,
                   UnsupportedInboundRecordTransformationException {
        var permission = permission(permissionId);
        var dataSource = dataSource(permission);

        return toDto(permission, latestRecord(permissionId, dataSource));
    }

    private InboundDataSource dataSource(InboundPermission permission) throws InboundRecordNotFoundException {
        var dataSource = permission.dataSource();
        if (dataSource == null) {
            throw new InboundRecordNotFoundException(permission.id());
        }
        return dataSource;
    }

    private InboundPermission permission(UUID permissionId) throws PermissionNotFoundException {
        LOGGER.trace("Getting latest raw inboundRecord for permission {}", permissionId);

        return inboundPermissionRepository.findById(permissionId)
                                          .orElseThrow(() -> new PermissionNotFoundException(permissionId));
    }

    private InboundRecord latestRecord(UUID permissionId, InboundDataSource dataSource) throws InboundRecordNotFoundException {
        return inboundRecordRepository
                .findTopByDataSourceIdOrderByTimestampDesc(dataSource.id())
                .orElseThrow(() -> new InboundRecordNotFoundException(permissionId, dataSource.id()));
    }

    private InboundRecordDto toDto(InboundPermission permission, InboundRecord inboundRecord)
            throws UnsupportedInboundRecordTransformationException {
        var format = permission.inboundMessageFormat();

        var dataSource = inboundRecord.dataSource();
        var payload = inboundPayloadTransformationService.transform(inboundRecord, format);

        return new InboundRecordDto(
                inboundRecord.timestamp(),
                dataSource.userId(),
                dataSource.id(),
                dataSource.asset(),
                dataSource.meterId(),
                dataSource.operatorId(),
                inboundRecord.schema(),
                format,
                payload
        );
    }
}
