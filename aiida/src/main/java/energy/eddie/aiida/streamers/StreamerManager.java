// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.streamers;

import energy.eddie.aiida.aggregator.OutboundAggregator;
import energy.eddie.aiida.models.permission.ActivePermission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.models.permission.dataneed.OutboundAiidaLocalDataNeed;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.PermissionLatestRecordMap;
import energy.eddie.aiida.repositories.FailedToSendRepository;
import energy.eddie.aiida.schemas.rtd.SchemaFormatterRegistry;
import energy.eddie.aiida.services.secrets.SecretsService;
import energy.eddie.api.agnostic.aiida.AiidaConnectionStatusMessageDto;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.cim.agnostic.PermissionCommand;
import jakarta.transaction.Transactional;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The StreamerManager manages the lifecycle of {@link AiidaStreamer}.
 * Other components should rely on the StreamerManager for creating or stopping streamers.
 */
@Component
public class StreamerManager implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamerManager.class);
    private final OutboundAggregator aggregator;
    private final FailedToSendRepository failedToSendRepository;
    private final ObjectMapper mapper;
    private final SchemaFormatterRegistry schemaFormatterRegistry;
    private final Map<UUID, AiidaStreamer> streamers;
    private final Sinks.Many<PermissionCommand> commands;
    private final PermissionLatestRecordMap permissionLatestRecordMap;
    private final SecretsService secretsService;
    private final ConnectionStatusEventStream connectionStatusEventStream;

    /**
     * The mapper is passed to the {@link AiidaStreamer} instances that which use it to convert POJOs to JSON.
     * As the mapper is shared, make sure the used implementation is thread-safe and supports sharing.
     */
    @Autowired
    public StreamerManager(
            OutboundAggregator aggregator,
            FailedToSendRepository failedToSendRepository,
            ObjectMapper mapper,
            SchemaFormatterRegistry schemaFormatterRegistry,
            PermissionLatestRecordMap permissionLatestRecordMap,
            SecretsService secretsService,
            ConnectionStatusEventStream connectionStatusEventStream
    ) {
        this.mapper = mapper;
        this.aggregator = aggregator;
        this.schemaFormatterRegistry = schemaFormatterRegistry;
        this.failedToSendRepository = failedToSendRepository;
        this.permissionLatestRecordMap = permissionLatestRecordMap;
        this.secretsService = secretsService;
        this.connectionStatusEventStream = connectionStatusEventStream;

        streamers = new ConcurrentHashMap<>();
        commands = Sinks.many().unicast().onBackpressureBuffer();
    }

    /**
     * Creates a new {@link AiidaStreamer} for the specified permission and stores it internally.
     * The created streamer will receive any matching {@link AiidaRecord} as requested by the data need of the permission.
     *
     * @param permission Permission for which an AiidaStreamer should be created.
     * @throws IllegalArgumentException If an AiidaStreamer for the passed permission has already been created.
     */
    @Transactional
    public void createNewStreamer(ActivePermission permission) throws IllegalArgumentException, MqttException {
        LOGGER.info("Will create a new AiidaStreamer for permission {}", permission.id());
        var id = permission.id();

        if (streamers.get(id) != null) {
            throw new IllegalStateException(
                    "An AiidaStreamer for EDDIE framework '%s' with permission '%s' has already been created.".formatted(
                            permission.eddieId(),
                            permission.id()));
        }

        var recordFlux = buildFilteredFlux(permission);
        var dependencies = new StreamerDependencies(failedToSendRepository,
                                                    mapper,
                                                    schemaFormatterRegistry,
                                                    commands,
                                                    permissionLatestRecordMap,
                                                    secretsService,
                                                    connectionStatusEventStream);

        var streamer = StreamerFactory.getAiidaStreamer(dependencies, permission, recordFlux);
        streamer.connect();
        streamers.put(id, streamer);
    }

    /**
     * Returns whether a streamer exists for the passed permission and is currently connected to the streaming target.
     */
    public boolean isConnected(UUID permissionId) {
        return requireStreamer(permissionId).map(AiidaStreamer::isConnected).orElse(false);
    }

    /**
     * Enables or disables data transmission for the streamer of the passed permission. While disabled, no records are
     * published to the EP.
     */
    public void setTransmissionEnabled(UUID permissionId, boolean enabled) {
        requireStreamer(permissionId)
                .ifPresent(streamer -> streamer.setTransmissionEnabled(enabled));
    }

    /**
     * Applies a new transmission schedule to the streamer of the passed permission by rebuilding its record flux with
     * the new aggregation cadence. No-op for permissions whose data need is not outbound, since only outbound
     * permissions currently have a record flux to rebuild.
     */
    public void updateSchedule(ActivePermission permission) {
        if (!(permission.dataNeed() instanceof OutboundAiidaLocalDataNeed)) {
            return;
        }

        requireStreamer(permission.id())
                .ifPresent(streamer -> streamer.updateRecordFlux(buildFilteredFlux(permission)));
    }

    /**
     * Publishes a payload of the provided schema to the streamer for the provided permission.
     */
    public void publishSchemaPayload(UUID permissionId, AiidaSchema schema, String payload) {
        requireStreamer(permissionId)
                .ifPresent(streamer -> streamer.publishSchemaPayload(permissionId, schema, payload));
    }

    /**
     * Returns a Flux on which a {@link PermissionCommand} is published whenever the EP sends a control command for one
     * of the managed permissions. The Flux allows only one subscriber and buffers, ensuring no values get lost.
     *
     * @return Flux of permission commands received from the EP.
     */
    public Flux<PermissionCommand> commandFlux() {
        return commands.asFlux();
    }

    /**
     * Sends the passed status message in a blocking manner and then terminally stops the streamer.
     *
     * @param message Message to send before stopping the streamer. The status should be one of
     *                {@link PermissionStatus#TERMINATED}, {@link PermissionStatus#REVOKED} or
     *                {@link PermissionStatus#FULFILLED}.
     */
    public void stopStreamer(AiidaConnectionStatusMessageDto message) {
        requireStreamer(message.permissionId())
                .ifPresent(streamer -> streamer.closeTerminally(message));
    }

    /**
     * Closes all streamers to allow for an orderly shutdown. Note that this blocks until all streamers have finished
     * closing, which may be indefinitely in the current implementation.
     */
    @Override
    public void close() {
        LOGGER.info("Closing all {} streamers", streamers.size());
        for (var entry : streamers.entrySet()) {
            entry.getValue().close();
        }

        commands.tryEmitComplete();
    }

    /**
     * Builds the filtered and aggregated record flux for the passed permission. Permissions that were not created by an
     * outbound data need get a flux that never emits, as they do not stream any records.
     */
    private Flux<AiidaRecord> buildFilteredFlux(ActivePermission permission) {
        if (!(permission.dataNeed() instanceof OutboundAiidaLocalDataNeed outboundDataNeed)) {
            return Flux.never();
        }

        return aggregator.getFilteredFlux(outboundDataNeed.dataTags(),
                                          outboundDataNeed.asset(),
                                          permission.expirationTime(),
                                          permission.transmissionSchedule(),
                                          permission.userId(),
                                          permission.dataSource().id());
    }

    private Optional<AiidaStreamer> requireStreamer(UUID permissionId) {
        return Optional.ofNullable(streamers.get(permissionId));
    }
}
