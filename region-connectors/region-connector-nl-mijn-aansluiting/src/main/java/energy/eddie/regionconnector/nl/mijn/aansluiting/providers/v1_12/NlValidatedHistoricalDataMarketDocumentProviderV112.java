// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v1_12;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.nl.mijn.aansluiting.config.MijnAansluitingConfiguration;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.PollingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Provides validated historical data as CIM v1.12 documents, based on the same metered data as the CIM v0.82 provider.
 */
@Component
public class NlValidatedHistoricalDataMarketDocumentProviderV112 {
    private static final Logger LOGGER = LoggerFactory.getLogger(NlValidatedHistoricalDataMarketDocumentProviderV112.class);
    private final Flux<Envelope> flux;

    public NlValidatedHistoricalDataMarketDocumentProviderV112(
            PollingService pollingService,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfig,
            MijnAansluitingConfiguration mijnAansluitingConfig
    ) {
        this.flux = pollingService.identifiableMeteredDataFlux()
                                  .flatMapIterable(data -> {
                                      try {
                                          return new IntermediateValidatedHistoricalDataMarketDocument(
                                                  cimConfig,
                                                  mijnAansluitingConfig,
                                                  data
                                          ).toVhds();
                                      } catch (Exception e) {
                                          LOGGER.warn("Caught exception when mapping metered data to CIM v1.12 VHD", e);
                                          return List.of();
                                      }
                                  });
    }

    @MessageStream(Envelope.class)
    public Flux<Envelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return flux;
    }
}
