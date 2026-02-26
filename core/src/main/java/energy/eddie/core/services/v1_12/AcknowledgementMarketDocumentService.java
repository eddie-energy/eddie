// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.services.v1_12;

import energy.eddie.api.v1_12.AcknowledgementMarketDocumentProvider;
import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service(value = "acknowledgementMarketDocumentService")
public class AcknowledgementMarketDocumentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcknowledgementMarketDocumentService.class);

    private final Sinks.Many<AcknowledgementEnvelope> ackSink = Sinks.many()
                                                                     .multicast()
                                                                     .onBackpressureBuffer();

    public void registerProvider(AcknowledgementMarketDocumentProvider provider) {
        LOGGER.info("Registering {}", provider.getClass().getName());
        provider.getAcknowledgementDataMarketDocumentsStream()
                .onErrorContinue((err, obj) -> LOGGER.warn(
                        "Encountered error while processing acknowledgement market document",
                        err))
                .subscribe(ackSink::tryEmitNext);
    }

    public Flux<AcknowledgementEnvelope> getAcknowledgementMarketDocumentStream() {
        return ackSink.asFlux();
    }
}