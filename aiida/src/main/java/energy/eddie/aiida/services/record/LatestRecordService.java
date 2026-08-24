// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.record;

import energy.eddie.aiida.aggregator.InboundAggregator;
import energy.eddie.aiida.dtos.record.*;
import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.errors.datasource.InvalidDataSourceTypeException;
import energy.eddie.aiida.errors.permission.InvalidInboundPermissionException;
import energy.eddie.aiida.errors.permission.LatestPermissionRecordNotFoundException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.errors.record.InboundRecordNotFoundException;
import energy.eddie.aiida.errors.record.LatestAiidaRecordNotFoundException;
import energy.eddie.aiida.errors.record.UnsupportedInboundRecordTransformationException;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.dataneed.InboundAiidaLocalDataNeed;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.AiidaRecordValue;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.models.record.PermissionLatestRecordMap;
import energy.eddie.aiida.repositories.AiidaRecordRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.services.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LatestRecordService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LatestRecordService.class);

    private final AiidaRecordRepository aiidaRecordRepository;
    private final PermissionLatestRecordMap permissionLatestRecordMap;
    private final InboundRecordService inboundRecordService;
    private final PermissionRepository permissionRepository;
    private final InboundAggregator inboundAggregator;
    private final AuthService authService;

    @Autowired
    public LatestRecordService(
            AiidaRecordRepository aiidaRecordRepository,
            PermissionLatestRecordMap permissionLatestRecordMap,
            InboundRecordService inboundRecordService,
            PermissionRepository permissionRepository,
            InboundAggregator inboundAggregator,
            AuthService authService
    ) {
        this.aiidaRecordRepository = aiidaRecordRepository;
        this.permissionLatestRecordMap = permissionLatestRecordMap;
        this.inboundRecordService = inboundRecordService;
        this.permissionRepository = permissionRepository;
        this.inboundAggregator = inboundAggregator;
        this.authService = authService;
    }

    public LatestDataSourceRecordDto latestDataSourceRecord(UUID dataSourceId) throws LatestAiidaRecordNotFoundException {
        var aiidaRecord = aiidaRecordRepository.findFirstByDataSourceIdOrderByIdDesc(dataSourceId)
                                               .orElseThrow(() -> new LatestAiidaRecordNotFoundException(dataSourceId));

        LOGGER.info("Found latest data source record with timestamp: {}, for data source: {}",
                    aiidaRecord.timestamp(),
                    dataSourceId);

        return recordToLatestDto(aiidaRecord);
    }

    public List<LatestDataSourceRecordDto> latestDataSourceRecords(
            UUID dataSourceId,
            int amount
    ) throws LatestAiidaRecordNotFoundException {
        var aiidaRecords = aiidaRecordRepository.findByDataSourceIdOrderByTimestampDesc(dataSourceId,
                                                                                        Pageable.ofSize(amount));
        if (aiidaRecords.isEmpty()) {
            throw new LatestAiidaRecordNotFoundException(dataSourceId);
        }

        LOGGER.info("Found data source record from timestamp: {} until {} for data source: {}",
                    aiidaRecords.getFirst().timestamp(),
                    aiidaRecords.getLast().timestamp(),
                    dataSourceId);

        return aiidaRecords
                .stream()
                .map(this::recordToLatestDto)
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
            throws PermissionNotFoundException, InvalidDataSourceTypeException,
                   InboundRecordNotFoundException, UnsupportedInboundRecordTransformationException,
                   InvalidInboundPermissionException {
        var inboundRecord = inboundRecordService.latestRecord(permissionId);

        return new LatestInboundPermissionRecordDto(
                inboundRecord.timestamp(),
                inboundRecord.dataSourceId(),
                inboundRecord.messageFormat(),
                inboundRecord.payload()
        );
    }

    /**
     * Streams a {@link LastMessageEventDto} for every new inbound/outbound message
     * belonging to the current user's permissions.
     * The stream filters per user and not per permission as HTTP/1.1 limits concurrent SSE streams to a maximum of 6.
     *
     * @throws InvalidUserException if the current request has no valid
     *                              authenticated user.
     */
    public Flux<LastMessageEventDto> lastMessageStream() throws InvalidUserException {
        var userId = authService.getCurrentUserId();
        var permissions = permissionRepository.findByUserIdOrderByGrantTimeDesc(userId);
        var dataSourceIdToPermissionIdMap = mapDataSourceIdToPermissionId(permissions);

        var inboundEvents = retrieveInboundEventsFlux(inboundAggregator.inboundRecordFlux(),
                                                      dataSourceIdToPermissionIdMap);
        var outboundEvents = retrieveOutboundEventsFlux(permissions);

        return combinedInboundAndOutboundEventsFlux(inboundEvents, outboundEvents, userId);
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

    private Flux<LastMessageEventDto> retrieveInboundEventsFlux(
            Flux<InboundRecord> inboundRecordFlux,
            Map<UUID, UUID> dataSourceIdToPermissionIdMap
    ) {
        return inboundRecordFlux.mapNotNull(inboundRecord -> {
            var permissionId = dataSourceIdToPermissionIdMap.get(inboundRecord.dataSource().id());
            return permissionId != null
                    ? new LastMessageEventDto(permissionId,
                                              inboundRecord.timestamp())
                    : null;
        });
    }

    private Flux<LastMessageEventDto> retrieveOutboundEventsFlux(List<Permission> permissions) {
        return Flux.merge(
                permissions.stream()
                           .filter(permission -> !(permission.dataNeed() instanceof InboundAiidaLocalDataNeed))
                           .map(permission -> permissionLatestRecordMap.lastMessageStream(permission.id())
                                                                       .map(timestamp -> new LastMessageEventDto(
                                                                               permission.id(),
                                                                               timestamp)))
                           .toList());
    }

    private Flux<LastMessageEventDto> combinedInboundAndOutboundEventsFlux(
            Flux<LastMessageEventDto> inboundEventsFlux,
            Flux<LastMessageEventDto> outboundEventsFlux,
            UUID userId
    ) {
        return Flux.merge(inboundEventsFlux, outboundEventsFlux)
                   .onErrorResume(throwable -> {
                       LOGGER.error("Error in last-message stream for user {}, terminating stream",
                                    userId,
                                    throwable);
                       return Flux.empty();
                   });
    }

    private Map<UUID, UUID> mapDataSourceIdToPermissionId(List<Permission> permissions) {
        return permissions.stream()
                          .filter(permission -> permission.dataNeed() instanceof InboundAiidaLocalDataNeed
                                                && permission.dataSource() != null)
                          .collect(Collectors.toMap(permission -> permission.dataSource().id(), Permission::id));
    }
}
