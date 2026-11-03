// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.inbound;

import energy.eddie.aiida.adapters.datasource.inbound.InboundMqttClientRegistry;
import energy.eddie.aiida.aggregator.InboundAggregator;
import energy.eddie.aiida.models.record.InboundProcessingResult;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.services.ApplicationInformationService;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;
import tools.jackson.databind.ObjectMapper;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dispatches inbound records to their schema-specific processing strategy and publishes the resulting acknowledgement.
 */
@Component
public class InboundProcessingDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(InboundProcessingDispatcher.class);
    private static final AiidaSchema ACK_SCHEMA = AiidaSchema.ACKNOWLEDGEMENT_CIM_V1_12;

    private final InboundAggregator inboundAggregator;
    private final InboundMqttClientRegistry mqttClientRegistry;
    private final ObjectMapper objectMapper;
    private final UUID aiidaId;
    private final Map<AiidaSchema, InboundProcessingStrategy> strategies;

    @Nullable
    private Disposable inboundRecordSubscription;

    public InboundProcessingDispatcher(
            InboundAggregator inboundAggregator,
            InboundMqttClientRegistry mqttClientRegistry,
            ObjectMapper objectMapper,
            ApplicationInformationService applicationInformationService,
            List<InboundProcessingStrategy> strategies
    ) {
        this.inboundAggregator = inboundAggregator;
        this.mqttClientRegistry = mqttClientRegistry;
        this.objectMapper = objectMapper;
        this.aiidaId = applicationInformationService.applicationInformation().aiidaId();
        this.strategies = strategiesBySchema(strategies);
    }

    @PostConstruct
    void subscribeToInboundRecords() {
        inboundRecordSubscription = inboundAggregator.inboundRecordFlux()
                                                     .publishOn(Schedulers.boundedElastic())
                                                     .subscribe(
                                                             this::dispatch,
                                                             error -> LOGGER.error("Inbound processing stream failed",
                                                                                   error)
                                                     );
    }

    void dispatch(InboundRecord inboundRecord) {
        var strategy = strategies.get(inboundRecord.schema());
        if (strategy == null) {
            LOGGER.error("No inbound processing strategy found for schema {}", inboundRecord.schema());
            return;
        }

        var processingResult = process(strategy, inboundRecord);
        publishAcknowledgement(strategy, inboundRecord, processingResult);
    }

    @PreDestroy
    void close() {
        if (inboundRecordSubscription != null) {
            inboundRecordSubscription.dispose();
        }
    }

    private InboundProcessingResult process(
            InboundProcessingStrategy strategy,
            InboundRecord inboundRecord
    ) {
        try {
            return strategy.process(inboundRecord);
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to process inbound record {}", inboundRecord.id(), exception);
            return InboundProcessingResult.rejected(
                    "AIIDA could not process the inbound document: " + errorMessage(exception)
            );
        }
    }

    private void publishAcknowledgement(
            InboundProcessingStrategy strategy,
            InboundRecord inboundRecord,
            InboundProcessingResult processingResult
    ) {
        var acknowledgementTopic = inboundRecord.dataSource().acknowledgementTopic();
        if (acknowledgementTopic == null) {
            LOGGER.debug("Skipping acknowledgement publishing because no acknowledgement topic is configured");
            return;
        }

        var mqttClient = mqttClientRegistry.clientFor(inboundRecord.dataSource().id());
        if (mqttClient.isEmpty()) {
            LOGGER.debug("Skipping acknowledgement publishing because no MQTT client is registered for data source {}",
                         inboundRecord.dataSource().id());
            return;
        }

        try {
            var topic = ACK_SCHEMA.buildTopicPath(acknowledgementTopic);
            var formatter = strategy.acknowledgementFormatter(aiidaId);
            var acknowledgementEnvelope = formatter.convert(objectMapper, inboundRecord, processingResult);
            var payload = objectMapper.writeValueAsBytes(acknowledgementEnvelope);

            LOGGER.debug("Publishing acknowledgement for record {} to topic {}", inboundRecord.id(), topic);
            mqttClient.orElseThrow().publish(topic, payload, 0, false);
        } catch (MqttException | RuntimeException exception) {
            LOGGER.error("Failed to publish acknowledgement for record {}", inboundRecord.id(), exception);
        }
    }

    private static Map<AiidaSchema, InboundProcessingStrategy> strategiesBySchema(
            List<InboundProcessingStrategy> strategies
    ) {
        var strategiesBySchema = new EnumMap<AiidaSchema, InboundProcessingStrategy>(AiidaSchema.class);
        for (var strategy : strategies) {
            var previous = strategiesBySchema.put(strategy.supportedSchema(), strategy);
            if (previous != null) {
                throw new IllegalStateException(
                        "Multiple inbound processing strategies configured for schema " + strategy.supportedSchema()
                );
            }
        }
        return Map.copyOf(strategiesBySchema);
    }

    private static String errorMessage(RuntimeException exception) {
        var message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
