// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.providers.v1_12;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.providers.IdentifiableValidatedHistoricalData;
import energy.eddie.regionconnector.de.eta.providers.ValidatedHistoricalDataStream;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * CIM v1.12 Validated Historical Data provider for the German ETA Plus connector.
 */
@Component("deValidatedHistoricalDataMarketDocumentProviderV112")
public class DeValidatedHistoricalDataMarketDocumentProviderV112 {

    private final Flux<IdentifiableValidatedHistoricalData> identifiableData;
    private final CommonInformationModelConfiguration cimConfig;
    private final DeEtaPlusConfiguration deConfiguration;

    public DeValidatedHistoricalDataMarketDocumentProviderV112(
            ValidatedHistoricalDataStream stream,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            CommonInformationModelConfiguration cimConfig,
            DeEtaPlusConfiguration deConfiguration
    ) {
        this.identifiableData = stream.validatedHistoricalData();
        this.cimConfig = cimConfig;
        this.deConfiguration = deConfiguration;
    }

    @MessageStream(Envelope.class)
    public Flux<Envelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return identifiableData
                .map(data -> new IntermediateValidatedHistoricalDataMarketDocument(cimConfig, deConfiguration, data))
                .flatMapIterable(IntermediateValidatedHistoricalDataMarketDocument::toVhd);
    }
}