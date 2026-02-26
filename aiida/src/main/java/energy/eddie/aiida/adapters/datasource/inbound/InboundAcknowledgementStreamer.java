// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.inbound;

import energy.eddie.aiida.errors.formatter.CimSchemaFormatterException;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.schemas.ack.MinMaxEnvelopeCimFormatterStrategy;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import jakarta.annotation.Nullable;
import org.eclipse.paho.mqttv5.client.IMqttAsyncClient;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Sinks;
import tools.jackson.databind.ObjectMapper;

public class InboundAcknowledgementStreamer {
    private static final Logger LOGGER = LoggerFactory.getLogger(InboundAcknowledgementStreamer.class);
    private static final AiidaSchema ACK_SCHEMA = AiidaSchema.ACKNOWLEDGEMENT_CIM_V1_12;

    private final ObjectMapper objectMapper;
    @Nullable
    private final String acknowledgementTopic;
    private final Sinks.Many<InboundRecord> inboundRecordSink;

    public InboundAcknowledgementStreamer(
            ObjectMapper objectMapper,
            @Nullable String acknowledgementTopic,
            Sinks.Many<InboundRecord> inboundRecordSink
    ) {
        this.objectMapper = objectMapper;
        this.acknowledgementTopic = acknowledgementTopic;
        this.inboundRecordSink = inboundRecordSink;
    }

    public void start(@Nullable IMqttAsyncClient mqttClient) {
        if (mqttClient != null && acknowledgementTopic != null) {
            inboundRecordSink.asFlux().subscribe(record -> publishAcknowledgement(mqttClient, record));
        }
    }

    private void publishAcknowledgement(IMqttAsyncClient mqttClient, InboundRecord inboundRecord) {
        try {
            LOGGER.info("Publishing acknowledgement for record {}", inboundRecord.id());

            var acknowledgementEnvelope = createToAcknowledgementEnvelope(inboundRecord);
            var payload = objectMapper.writeValueAsBytes(acknowledgementEnvelope);
            var topic = ACK_SCHEMA.buildTopicPath(acknowledgementTopic);

            mqttClient.publish(topic, payload, 0, false);
        } catch (CimSchemaFormatterException | MqttException e) {
            LOGGER.error("Failed to publish acknowledgement for record {}", inboundRecord.id(), e);
        }
    }

    private AcknowledgementEnvelope createToAcknowledgementEnvelope(
            InboundRecord inboundRecord
    ) throws CimSchemaFormatterException {
        var strategy = switch (inboundRecord.schema()) {
            case MIN_MAX_ENVELOPE_CIM_V1_12 -> new MinMaxEnvelopeCimFormatterStrategy();
            default -> throw new CimSchemaFormatterException(new IllegalArgumentException(
                    "No CIM formatter strategy found for schema " + inboundRecord.schema()));
        };

        return strategy.convert(objectMapper, inboundRecord);
    }
}
