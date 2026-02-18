// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.dtos.record.LatestDataSourceRecordDto;
import energy.eddie.aiida.dtos.record.LatestInboundPermissionRecordDto;
import energy.eddie.aiida.dtos.record.LatestOutboundPermissionRecordDto;
import energy.eddie.aiida.dtos.record.LatestSchemaRecordDto;
import energy.eddie.aiida.errors.datasource.InvalidDataSourceTypeException;
import energy.eddie.aiida.errors.permission.LatestPermissionRecordNotFoundException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.errors.record.InboundRecordNotFoundException;
import energy.eddie.aiida.errors.record.LatestAiidaRecordNotFoundException;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.models.record.PermissionLatestRecordMap;
import energy.eddie.aiida.repositories.AiidaRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class LatestRecordService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LatestRecordService.class);

    private final AiidaRecordRepository aiidaRecordRepository;
    private final PermissionLatestRecordMap permissionLatestRecordMap;
    private final InboundService inboundService;

    @Autowired
    public LatestRecordService(
            AiidaRecordRepository aiidaRecordRepository,
            PermissionLatestRecordMap permissionLatestRecordMap,
            InboundService inboundService
    ) {
        this.aiidaRecordRepository = aiidaRecordRepository;
        this.permissionLatestRecordMap = permissionLatestRecordMap;
        this.inboundService = inboundService;
    }

    public LatestDataSourceRecordDto latestDataSourceRecord(UUID dataSourceId) throws LatestAiidaRecordNotFoundException {
        var aiidaRecord = aiidaRecordRepository.findFirstByDataSourceIdOrderByIdDesc(dataSourceId)
                                               .orElseThrow(() -> new LatestAiidaRecordNotFoundException(dataSourceId));

        LOGGER.info("Found latest data source record with timestamp: {}, for data source: {}",
                    aiidaRecord.timestamp(),
                    dataSourceId);

        return recordToLatestDto(aiidaRecord);
    }

    public LatestOutboundPermissionRecordDto latestOutboundPermissionRecord(UUID permissionId) throws LatestPermissionRecordNotFoundException {
        var permissionRecord = permissionLatestRecordMap
                .get(permissionId)
                .orElseThrow(() -> new LatestPermissionRecordNotFoundException(permissionId));
        var messages = permissionRecord.messages()
                                       .entrySet()
                                       .stream()
                                       .map(latestRecord -> {
                                           var message = latestRecord.getValue();
                                           return new LatestSchemaRecordDto(latestRecord.getKey(),
                                                                            message.sentAt(),
                                                                            message.message());
                                       })
                                       .toList();

        return new LatestOutboundPermissionRecordDto(
                permissionId,
                permissionRecord.topic(),
                permissionRecord.serverUri(),
                messages
        );
    }

    public LatestInboundPermissionRecordDto latestInboundPermissionRecord(UUID permissionId)
            throws PermissionNotFoundException, InvalidDataSourceTypeException, InboundRecordNotFoundException {
        var inboundRecord = inboundService.latestRecord(permissionId);

        return new LatestInboundPermissionRecordDto(
                inboundRecord.timestamp(),
                inboundRecord.dataSource().id(),
                inboundRecord.payload()
        );
    }

    private LatestDataSourceRecordDto recordToLatestDto(AiidaRecord aiidaRecord) {
        var dataSource = aiidaRecord.dataSource();

        return new LatestDataSourceRecordDto(aiidaRecord.timestamp(),
                                             dataSource.name(),
                                             dataSource.asset(),
                                             dataSource.id(),
                                             aiidaRecord.aiidaRecordValues()
                                                        .stream()
                                                        .map(AiidaRecordValue::toDto)
                                                        .toList());
    }
}
