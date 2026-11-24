// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.inbound;

import energy.eddie.aiida.adapters.datasource.inbound.InboundMqttClientRegistry;
import energy.eddie.aiida.adapters.datasource.inbound.ack.AckFormatterStrategy;
import energy.eddie.aiida.models.record.InboundProcessingResult;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Publishes acknowledgements for processed inbound records using the matching schema formatter.
 */
@Component
public class AcknowledgementPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcknowledgementPublisher.class);
    private static final AiidaSchema ACK_SCHEMA = AiidaSchema.ACKNOWLEDGEMENT_CIM_V1_12;

    private final InboundMqttClientRegistry mqttClientRegistry;
    private final ObjectMapper objectMapper;
    private final Map<AiidaSchema, AckFormatterStrategy> formatters;

    public AcknowledgementPublisher(
            InboundMqttClientRegistry mqttClientRegistry,
            ObjectMapper objectMapper,
            List<AckFormatterStrategy> formatters
    ) {
        this.mqttClientRegistry = mqttClientRegistry;
        this.objectMapper = objectMapper;
        this.formatters = formatters.stream()
                                    .collect(Collectors.toMap(AckFormatterStrategy::supportedSchema,
                                                              formatter -> formatter));
    }

    public void publish(InboundRecord inboundRecord, InboundProcessingResult processingResult) {
        var dataSource = inboundRecord.dataSource();

        var acknowledgementTopic = dataSource.acknowledgementTopic();
        if (acknowledgementTopic == null) {
            LOGGER.debug("Skipping acknowledgement publishing because no acknowledgement topic is configured");
            return;
        }

        var formatter = formatters.get(inboundRecord.schema());
        if (formatter == null) {
            LOGGER.error("No acknowledgement formatter found for schema {}", inboundRecord.schema());
            return;
        }

        var mqttClient = mqttClientRegistry.clientFor(dataSource.id());
        if (mqttClient.isEmpty()) {
            LOGGER.debug("Skipping acknowledgement publishing because no MQTT client is registered for data source {}",
                         dataSource.id());
            return;
        }

        try {
            var topic = ACK_SCHEMA.buildTopicPath(acknowledgementTopic);
            var acknowledgementEnvelope = formatter.convert(inboundRecord, processingResult);
            var payload = objectMapper.writeValueAsBytes(acknowledgementEnvelope);

            LOGGER.debug("Publishing acknowledgement for record {} to topic {}", inboundRecord.id(), topic);
            mqttClient.orElseThrow().publish(topic, payload, 0, false);
        } catch (MqttException | RuntimeException exception) {
            LOGGER.error("Failed to publish acknowledgement for record {}", inboundRecord.id(), exception);
        }
    }
}
