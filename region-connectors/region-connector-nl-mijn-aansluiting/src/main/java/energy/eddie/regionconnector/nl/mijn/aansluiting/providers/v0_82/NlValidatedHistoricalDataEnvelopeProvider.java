// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.providers.v0_82;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.regionconnector.nl.mijn.aansluiting.config.MijnAansluitingConfiguration;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.IdentifiableMeteredData;
import energy.eddie.regionconnector.nl.mijn.aansluiting.services.PollingService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class NlValidatedHistoricalDataEnvelopeProvider {
    private final Flux<ValidatedHistoricalDataEnvelope> flux;
    private final CommonInformationModelConfiguration cimConfig;
    private final MijnAansluitingConfiguration mijnAansluitingConfig;

    public NlValidatedHistoricalDataEnvelopeProvider(
            PollingService pollingService,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfig,
            MijnAansluitingConfiguration mijnAansluitingConfig
    ) {
        this.flux = pollingService.identifiableMeteredDataFlux()
                                  .flatMap(this::mapToVhd);
        this.cimConfig = cimConfig;
        this.mijnAansluitingConfig = mijnAansluitingConfig;
    }

    private Flux<ValidatedHistoricalDataEnvelope> mapToVhd(IdentifiableMeteredData identifiableMeteredData) {
        IntermediateValidatedHistoricalDataMarketDocument vhd = new IntermediateValidatedHistoricalDataMarketDocument(
                cimConfig,
                mijnAansluitingConfig,
                identifiableMeteredData
        );
        var docs = vhd.toEddieValidatedHistoricalDataMarketDocuments();
        if (docs.isEmpty()) {
            return Flux.empty();
        }
        return Flux.fromIterable(docs);
    }

    @MessageStream(ValidatedHistoricalDataEnvelope.class)
    public Flux<ValidatedHistoricalDataEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return flux;
    }
}
