// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.services.v1_12;

import energy.eddie.api.v1_12.EnergySharingReferenceDataMarketDocumentProvider;
import energy.eddie.cim.v1_12.esr.ESRDMDEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service(value = "energySharingReferenceDataMarketDocumentServiceV112")
public class EnergySharingReferenceDataMarketDocumentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnergySharingReferenceDataMarketDocumentService.class);

    private final Sinks.Many<ESRDMDEnvelope> sink = Sinks.many()
                                                         .multicast()
                                                         .onBackpressureBuffer();

    public void registerProvider(EnergySharingReferenceDataMarketDocumentProvider provider) {
        LOGGER.info("Registering {}", provider.getClass().getName());
        provider.getEnergySharingReferenceDataMarketDocumentStream()
                .onErrorContinue((err, obj) -> LOGGER.warn(
                        "Encountered error while processing energy sharing reference data market document",
                        err))
                .subscribe(sink::tryEmitNext);
    }

    public Flux<ESRDMDEnvelope> getEnergySharingReferenceDataMarketDocumentStream() {
        return sink.asFlux();
    }
}