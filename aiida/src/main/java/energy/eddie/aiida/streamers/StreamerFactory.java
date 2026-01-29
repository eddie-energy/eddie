// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.streamers;

import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.models.record.PermissionLatestRecordMap;
import energy.eddie.aiida.repositories.FailedToSendRepository;
import energy.eddie.aiida.streamers.mqtt.MqttStreamer;
import energy.eddie.aiida.streamers.mqtt.MqttStreamingContext;
import energy.eddie.aiida.utils.MqttFactory;
import org.eclipse.paho.mqttv5.client.persist.MqttDefaultFilePersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.springframework.web.util.UriTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class StreamerFactory {

    private StreamerFactory() {
    }

    /**
     * Creates a new {@link AiidaStreamer} applying the specified streamingConfig.
     *
     * @param aiidaId                UUID of the AIIDA instance for which to create the AiidaStreamer.
     * @param failedToSendRepository Repository to save messages that could not be sent.
     * @param mapper                 {@link ObjectMapper} that should be used to convert the records to JSON.
     * @param permission             Permission for which to create the AiidaStreamer.
     * @param recordFlux             Flux on which the records that should be sent are published.
     * @param terminationRequestSink Sink, to which the permissionId will be published when the EP requests a
     *                               termination.
     * @throws MqttException If the creation of the MqttClient failed.
     */
    protected static AiidaStreamer getAiidaStreamer(
            UUID aiidaId,
            FailedToSendRepository failedToSendRepository,
            ObjectMapper mapper,
            Permission permission,
            Flux<AiidaRecord> recordFlux,
            Sinks.One<UUID> terminationRequestSink,
            PermissionLatestRecordMap permissionLatestRecordMap
    ) throws MqttException {
        var mqttFilePersistenceDirectory = "mqtt-persistence/{eddieId}/{permissionId}";
        var streamingConfig = requireNonNull(permission.mqttStreamingConfig());
        var client = MqttFactory.getMqttAsyncClient(streamingConfig.serverUri(),
                                                    streamingConfig.username().toString(),
                                                    new MqttDefaultFilePersistence(new UriTemplate(
                                                            mqttFilePersistenceDirectory).expand(permission.eddieId(),
                                                                                                 permission.id())
                                                                                         .getPath()));
        var streamingContext = new MqttStreamingContext(client, streamingConfig, permissionLatestRecordMap);

        return new MqttStreamer(aiidaId,
                                failedToSendRepository,
                                mapper,
                                permission,
                                recordFlux,
                                streamingContext,
                                terminationRequestSink);
    }
}
