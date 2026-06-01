// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.record;

import energy.eddie.aiida.dtos.record.InboundRecordDto;
import energy.eddie.aiida.errors.auth.UnauthorizedException;
import energy.eddie.aiida.errors.datasource.InvalidDataSourceTypeException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.errors.record.InboundRecordNotFoundException;
import energy.eddie.aiida.errors.record.UnsupportedInboundRecordTransformationException;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.permission.InboundMessageFormat;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.repositories.InboundRecordRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.services.record.transform.InboundPayloadTransformationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
public class InboundRecordService {
    private static final Logger LOGGER = LoggerFactory.getLogger(InboundRecordService.class);

    private final InboundRecordRepository inboundRecordRepository;
    private final PermissionRepository permissionRepository;
    private final InboundPayloadTransformationService inboundPayloadTransformationService;

    public InboundRecordService(
            InboundRecordRepository inboundRecordRepository,
            PermissionRepository permissionRepository,
            InboundPayloadTransformationService inboundPayloadTransformationService
    ) {
        this.inboundRecordRepository = inboundRecordRepository;
        this.permissionRepository = permissionRepository;
        this.inboundPayloadTransformationService = inboundPayloadTransformationService;
    }

    public InboundRecordDto latestRecord(UUID permissionId, String accessCode)
            throws PermissionNotFoundException, UnauthorizedException,
                   InvalidDataSourceTypeException, InboundRecordNotFoundException, UnsupportedInboundRecordTransformationException {
        var permission = permission(permissionId);
        var dataSource = dataSource(permission);
        if (!Objects.equals(dataSource.accessCode(), accessCode)) {
            throw new UnauthorizedException(
                    "Access code does not match for data source with ID: " + dataSource.id()
            );
        }

        return toDto(latestRecord(dataSource), permission.inboundMessageFormat());
    }

    public InboundRecordDto latestRecord(UUID permissionId)
            throws PermissionNotFoundException, InvalidDataSourceTypeException, InboundRecordNotFoundException, UnsupportedInboundRecordTransformationException {
        var permission = permission(permissionId);
        var dataSource = dataSource(permission);
        return toDto(latestRecord(dataSource), permission.inboundMessageFormat());
    }

    private InboundDataSource dataSource(Permission permission) throws InvalidDataSourceTypeException {
        var dataSource = permission.dataSource();
        if (!(dataSource instanceof InboundDataSource inboundDataSource)) {
            throw new InvalidDataSourceTypeException();
        }
        return inboundDataSource;
    }

    private Permission permission(UUID permissionId) throws PermissionNotFoundException {
        LOGGER.trace("Getting latest raw inboundRecord for permission {}", permissionId);

        return permissionRepository.findById(permissionId)
                                   .orElseThrow(() -> new PermissionNotFoundException(permissionId));
    }

    private InboundRecord latestRecord(InboundDataSource dataSource) throws InboundRecordNotFoundException {
        return inboundRecordRepository
                .findTopByDataSourceIdOrderByTimestampDesc(dataSource.id())
                .orElseThrow(() -> new InboundRecordNotFoundException(dataSource.id()));
    }

    private InboundRecordDto toDto(InboundRecord inboundRecord, InboundMessageFormat format)
            throws UnsupportedInboundRecordTransformationException {

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
