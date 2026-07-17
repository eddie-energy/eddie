// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.streamers;

import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.record.AiidaRecord;
import energy.eddie.aiida.streamers.mqtt.MqttStreamer;
import energy.eddie.aiida.streamers.mqtt.MqttStreamingContext;
import energy.eddie.aiida.utils.MqttFactory;
import org.eclipse.paho.mqttv5.client.persist.MqttDefaultFilePersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.springframework.web.util.UriTemplate;
import reactor.core.publisher.Flux;

import static java.util.Objects.requireNonNull;

public class StreamerFactory {

    private StreamerFactory() {
    }

    /**
     * Creates a new {@link AiidaStreamer} applying the specified streamingConfig.
     *
     * @param dependencies Streamer dependencies shared by every streamer implementation.
     * @param permission   Permission for which to create the AiidaStreamer.
     * @param recordFlux   Flux on which the records that should be sent are published.
     * @throws MqttException If the creation of the MqttClient failed.
     */
    protected static AiidaStreamer getAiidaStreamer(
            StreamerDependencies dependencies,
            Permission permission,
            Flux<AiidaRecord> recordFlux
    ) throws MqttException {
        var mqttFilePersistenceDirectory = "mqtt-persistence/{eddieId}/{permissionId}";
        var streamingConfig = requireNonNull(permission.mqttStreamingConfig());
        var client = MqttFactory.getMqttAsyncClient(streamingConfig.serverUri(),
                                                    streamingConfig.username().toString(),
                                                    new MqttDefaultFilePersistence(new UriTemplate(
                                                            mqttFilePersistenceDirectory).expand(permission.eddieId(),
                                                                                                 permission.id())
                                                                                         .getPath()));
        var streamingContext = new MqttStreamingContext(client, streamingConfig, dependencies.permissionLatestRecordMap());

        return new MqttStreamer(dependencies, permission, recordFlux, streamingContext);
    }
}
