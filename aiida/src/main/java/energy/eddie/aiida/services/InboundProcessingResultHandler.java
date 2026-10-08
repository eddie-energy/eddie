// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.models.record.InboundProcessingResult;
import energy.eddie.aiida.models.record.InboundRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Handles a completed inbound processing result and returns it to the matching acknowledgement publisher.
 */
@Component
public class InboundProcessingResultHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(InboundProcessingResultHandler.class);

    private final Sinks.Many<ProcessedInboundRecord> sink = Sinks.many().multicast().onBackpressureBuffer();

    public void handle(InboundRecord inboundRecord, InboundProcessingResult processingResult) {
        var emissionResult = sink.tryEmitNext(new ProcessedInboundRecord(inboundRecord, processingResult));
        if (emissionResult.isFailure()) {
            LOGGER.warn("Could not handle processing result for inbound record {}: {}",
                        inboundRecord.id(),
                        emissionResult);
        }
    }

    public Flux<ProcessedInboundRecord> flux() {
        return sink.asFlux();
    }

    public record ProcessedInboundRecord(
            InboundRecord inboundRecord,
            InboundProcessingResult processingResult
    ) {}
}
