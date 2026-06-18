// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.inbound.ack;

import energy.eddie.aiida.errors.formatter.CimSchemaFormatterException;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import jakarta.annotation.Nullable;
import org.eclipse.paho.mqttv5.client.IMqttAsyncClient;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

public class InboundAcknowledgementPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(InboundAcknowledgementPublisher.class);
    private static final AiidaSchema ACK_SCHEMA = AiidaSchema.ACKNOWLEDGEMENT_CIM_V1_12;

    private final UUID aiidaId;
    private final ObjectMapper objectMapper;
    @Nullable
    private final String acknowledgementTopic;
    private final AckFormatterStrategyRegistry ackFormatterStrategyRegistry;
    @Nullable
    private IMqttAsyncClient mqttClient;

    public InboundAcknowledgementPublisher(
            UUID aiidaId,
            ObjectMapper objectMapper,
            @Nullable String acknowledgementTopic
    ) {
        this(aiidaId,
             objectMapper,
             acknowledgementTopic,
             new AckFormatterStrategyRegistry());
    }

    public InboundAcknowledgementPublisher(
            UUID aiidaId,
            ObjectMapper objectMapper,
            @Nullable String acknowledgementTopic,
            AckFormatterStrategyRegistry ackFormatterStrategyRegistry
    ) {
        this.aiidaId = aiidaId;
        this.objectMapper = objectMapper;
        this.acknowledgementTopic = acknowledgementTopic;
        this.ackFormatterStrategyRegistry = ackFormatterStrategyRegistry;
    }

    public void setMqttClient(@Nullable IMqttAsyncClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    public void publishAcknowledgement(InboundRecord inboundRecord) {
        if (mqttClient == null || acknowledgementTopic == null) {
            LOGGER.debug("Skipping acknowledgement publishing because mqttClient or ackTopic is null");
            return;
        }

        try {
            var topic = ACK_SCHEMA.buildTopicPath(acknowledgementTopic);
            LOGGER.debug("Publishing acknowledgement for record {} to topic {}", inboundRecord.id(), topic);

            var strategy = ackFormatterStrategyRegistry.strategyFor(inboundRecord.schema(), aiidaId);
            var acknowledgementEnvelope = strategy.convert(objectMapper, inboundRecord);
            var payload = objectMapper.writeValueAsBytes(acknowledgementEnvelope);

            mqttClient.publish(topic, payload, 0, false);
        } catch (CimSchemaFormatterException | MqttException e) {
            LOGGER.error("Failed to publish acknowledgement for record {}", inboundRecord.id(), e);
        }
    }
}
