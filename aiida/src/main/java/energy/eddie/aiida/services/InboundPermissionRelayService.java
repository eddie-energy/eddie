// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.aggregator.InboundAggregator;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.streamers.StreamerManager;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.cim.agnostic.OpaqueEnvelope;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;
import java.util.UUID;

@Service
public class InboundPermissionRelayService {
    private static final Logger LOGGER = LoggerFactory.getLogger(InboundPermissionRelayService.class);

    private final InboundAggregator inboundAggregator;
    private final PermissionRepository permissionRepository;
    private final StreamerManager streamerManager;
    private final ObjectMapper objectMapper;

    public InboundPermissionRelayService(
            InboundAggregator inboundAggregator,
            PermissionRepository permissionRepository,
            StreamerManager streamerManager,
            ObjectMapper objectMapper
    ) {
        this.inboundAggregator = inboundAggregator;
        this.permissionRepository = permissionRepository;
        this.streamerManager = streamerManager;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void subscribeToInboundRecords() {
        inboundAggregator.inboundRecordFlux()
                        .publishOn(Schedulers.boundedElastic())
                        .doOnNext(this::relayInboundRecord)
                        .onErrorContinue((error, value) -> LOGGER.error("Inbound relay dropped record {}", value, error))
                        .subscribe();
    }

    private void relayInboundRecord(InboundRecord inboundRecord) {
        var outboundPermissions = permissionRepository.findOutboundByDataSourceIdAndStatus(
                inboundRecord.dataSource().id(),
                PermissionStatus.ACTIVE
        );

        for (var outboundPermission : outboundPermissions) {
            relayToOutboundPermission(inboundRecord, outboundPermission);
        }
    }

    private void relayToOutboundPermission(InboundRecord inboundRecord, Permission outboundPermission) {
        if (!isSchemaRequested(outboundPermission, inboundRecord.schema())) {
            return;
        }

        try {
            var rewrittenPayload = switch (inboundRecord.schema()) {
                case OPAQUE -> rewriteOpaquePayload(inboundRecord.payload(), outboundPermission);
                case MIN_MAX_ENVELOPE_CIM_V1_12 -> rewriteMinMaxPayload(inboundRecord.payload(), outboundPermission);
                default -> null;
            };

            if (rewrittenPayload != null) {
                streamerManager.publishSchemaPayload(outboundPermission.id(), inboundRecord.schema(), rewrittenPayload);
            }
        } catch (NullPointerException | JacksonException exception) {
            LOGGER.error("Failed to rewrite {} payload for inbound message when forwarding to outbound permission {}",
                         inboundRecord.schema(),
                         outboundPermission.id(),
                         exception);
        }
    }

    private boolean isSchemaRequested(Permission outboundPermission, AiidaSchema schema) {
        var dataNeed = outboundPermission.dataNeed();
        return dataNeed != null && dataNeed.schemas() != null && dataNeed.schemas().contains(schema);
    }

    private String rewriteOpaquePayload(String payload, Permission outboundPermission) {
        var inboundEnvelope = objectMapper.readValue(payload, OpaqueEnvelope.class);

        var outboundEnvelope = new OpaqueEnvelope(
                inboundEnvelope.regionConnectorId(),
                outboundPermission.id().toString(),
                outboundPermission.connectionId(),
                Objects.requireNonNull(outboundPermission.dataNeed()).dataNeedId().toString(),
                UUID.randomUUID().toString(),
                inboundEnvelope.timestamp(),
                inboundEnvelope.payload()
        );

        return objectMapper.writeValueAsString(outboundEnvelope);
    }

    private String rewriteMinMaxPayload(String payload, Permission outboundPermission) {
        var minMaxEnvelope = objectMapper.readValue(payload, RECMMOEEnvelope.class);
        var header = minMaxEnvelope.getMessageDocumentHeader();
        var meta = header.getMetaInformation();

        meta.setRequestPermissionId(outboundPermission.id().toString());
        meta.setConnectionId(outboundPermission.connectionId());
        meta.setDataNeedId(Objects.requireNonNull(outboundPermission.dataNeed()).dataNeedId().toString());

        header.setMetaInformation(meta);
        minMaxEnvelope.setMessageDocumentHeader(header);

        return objectMapper.writeValueAsString(minMaxEnvelope);
    }
}
