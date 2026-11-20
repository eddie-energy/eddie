// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.inbound;

import energy.eddie.aiida.aggregator.InboundAggregator;
import energy.eddie.aiida.models.record.InboundProcessingResult;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dispatches inbound records to their schema-specific processing strategy and publishes the resulting acknowledgement.
 */
@Component
public class InboundProcessingDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(InboundProcessingDispatcher.class);

    private final InboundAggregator inboundAggregator;
    private final AcknowledgementPublisher acknowledgementPublisher;
    private final Map<AiidaSchema, InboundProcessingStrategy> strategies;

    @Nullable
    private Disposable inboundRecordSubscription;

    public InboundProcessingDispatcher(
            InboundAggregator inboundAggregator,
            AcknowledgementPublisher acknowledgementPublisher,
            List<InboundProcessingStrategy> strategies
    ) {
        this.inboundAggregator = inboundAggregator;
        this.acknowledgementPublisher = acknowledgementPublisher;
        this.strategies = strategies.stream()
                                    .collect(Collectors.toMap(InboundProcessingStrategy::supportedSchema,
                                                              strategy -> strategy));
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
        acknowledgementPublisher.publish(inboundRecord, processingResult);
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

    private static String errorMessage(RuntimeException exception) {
        var message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
