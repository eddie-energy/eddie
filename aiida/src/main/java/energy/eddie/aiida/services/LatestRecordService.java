// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.dtos.record.LatestDataSourceRecordDto;
import energy.eddie.aiida.dtos.record.LatestInboundPermissionRecordDto;
import energy.eddie.aiida.dtos.record.LatestOutboundPermissionRecordDto;
import energy.eddie.aiida.dtos.record.LatestSchemaRecordDto;
import energy.eddie.aiida.errors.datasource.DataSourceNotFoundException;
import energy.eddie.aiida.errors.datasource.InvalidDataSourceTypeException;
import energy.eddie.aiida.errors.permission.LatestPermissionRecordNotFoundException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.errors.record.InboundRecordNotFoundException;
import energy.eddie.aiida.errors.record.LatestAiidaRecordNotFoundException;
import energy.eddie.aiida.models.record.PermissionLatestRecordMap;
import energy.eddie.aiida.repositories.AiidaRecordRepository;
import energy.eddie.aiida.repositories.DataSourceRepository;
import energy.eddie.aiida.utils.AiidaRecordConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class LatestRecordService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LatestRecordService.class);

    private final AiidaRecordRepository aiidaRecordRepository;
    private final DataSourceRepository dataSourceRepository;
    private final PermissionLatestRecordMap permissionLatestRecordMap;
    private final InboundService inboundService;

    @Autowired
    public LatestRecordService(
            AiidaRecordRepository aiidaRecordRepository,
            DataSourceRepository dataSourceRepository,
            PermissionLatestRecordMap permissionLatestRecordMap,
            InboundService inboundService
    ) {
        this.aiidaRecordRepository = aiidaRecordRepository;
        this.dataSourceRepository = dataSourceRepository;
        this.permissionLatestRecordMap = permissionLatestRecordMap;
        this.inboundService = inboundService;
    }

    public LatestDataSourceRecordDto latestDataSourceRecord(UUID dataSourceId) throws LatestAiidaRecordNotFoundException, DataSourceNotFoundException {
        var dataSource = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new DataSourceNotFoundException(dataSourceId)
                );
        var aiidaRecord = aiidaRecordRepository.findFirstByDataSourceIdOrderByIdDesc(dataSourceId)
                .orElseThrow(() -> new LatestAiidaRecordNotFoundException(dataSourceId)
                );

        LOGGER.info("Found latest data source record with timestamp: {}, for data source: {}",
                aiidaRecord.timestamp(),
                dataSource.id());

        return AiidaRecordConverter.recordToLatestDto(aiidaRecord, dataSource);
    }

    public List<LatestDataSourceRecordDto> latestDataSourceRecords(UUID dataSourceId, int amount)
            throws LatestAiidaRecordNotFoundException, DataSourceNotFoundException {
        var dataSource = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new DataSourceNotFoundException(dataSourceId)
                );
        var aiidaRecords = aiidaRecordRepository.findByDataSourceIdOrderByTimestampDesc(dataSourceId, Pageable.ofSize(amount));
        if (aiidaRecords.isEmpty()) {
            throw new LatestAiidaRecordNotFoundException(dataSourceId);
        }

        LOGGER.info("Found data source record from timestamp: {} until {} for data source: {}",
                aiidaRecords.getFirst().timestamp(),
                aiidaRecords.getLast().timestamp(),
                dataSource.id());

        return aiidaRecords
                .stream()
                .map(aiidaRecord -> AiidaRecordConverter.recordToLatestDto(aiidaRecord, dataSource))
                .toList();
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
                inboundRecord.asset(),
                inboundRecord.payload()
        );
    }
}
