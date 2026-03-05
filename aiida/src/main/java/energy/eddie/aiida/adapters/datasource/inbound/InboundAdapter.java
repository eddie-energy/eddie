// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.inbound;

import energy.eddie.aiida.adapters.datasource.MqttDataSourceAdapter;
import energy.eddie.aiida.adapters.datasource.inbound.ack.InboundAcknowledgementStreamer;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

public class InboundAdapter extends MqttDataSourceAdapter<InboundDataSource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InboundAdapter.class);
    private final Sinks.Many<InboundRecord> inboundRecordSink;
    private final InboundAcknowledgementStreamer acknowledgementStreamer;

    /**
     * Creates the adapter for the inbound data source. It connects to the specified MQTT broker and expects that the
     * EP publishes its messages on the specified topic.
     *
     * @param dataSource        The entity of the data source.
     * @param mapper            The object mapper to use for parsing the messages and formatting the acknowledgements.
     * @param mqttConfiguration The MQTT configuration to use for connecting to the broker.
     * @param aiidaId           The ID of the AiiDA instance, used for formatting the acknowledgements.
     */
    public InboundAdapter(
            InboundDataSource dataSource,
            ObjectMapper mapper,
            MqttConfiguration mqttConfiguration,
            UUID aiidaId
    ) {
        super(dataSource, LOGGER, mqttConfiguration);
        inboundRecordSink = Sinks.many().multicast().onBackpressureBuffer();

        acknowledgementStreamer = new InboundAcknowledgementStreamer(
                aiidaId,
                mapper,
                dataSource.acknowledgementTopic(),
                inboundRecordSink.asFlux());
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        acknowledgementStreamer.start(asyncClient);
        super.connectComplete(reconnect, serverURI);
    }

    /**
     * MQTT callback function that is called when a new message from the broker is received. Will store the message in
     * plaintext format in the database, as this datasource is not designed to parse the messages.
     *
     * @param topic   Name of the topic, the message was published to.
     * @param message The actual message.
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        LOGGER.trace("Received message on topic {} from data source {}", topic, dataSource().name());

        var schema = AiidaSchema.forTopic(topic);
        if (schema.isEmpty()) {
            LOGGER.error("Received message with invalid topic: {}", topic);
            return;
        }

        var inboundRecord = new InboundRecord(
                Instant.now(),
                dataSource(),
                schema.get(),
                new String(message.getPayload(), StandardCharsets.UTF_8)
        );
        inboundRecordSink.tryEmitNext(inboundRecord);
    }

    @Override
    public void deliveryComplete(IMqttToken token) {
        LOGGER.trace("Delivery complete for MqttToken {}", token);
    }

    public Flux<InboundRecord> inboundRecordFlux() {
        return inboundRecordSink.asFlux();
    }

    @Override
    protected MqttConnectionOptions createConnectOptions() {
        var connectOptions = super.createConnectOptions();

        connectOptions.setUserName(dataSource().username());
        connectOptions.setPassword(dataSource().password().getBytes(StandardCharsets.UTF_8));

        return connectOptions;
    }
}