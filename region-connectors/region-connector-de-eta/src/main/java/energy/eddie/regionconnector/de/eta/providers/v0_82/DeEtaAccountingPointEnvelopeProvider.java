// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.providers.v0_82;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.providers.AccountingPointDataStream;
import energy.eddie.regionconnector.de.eta.providers.IdentifiableAccountingPointData;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * CIM v0.82 Accounting Point provider for the German ETA Plus connector.
 */
@Component
public class DeEtaAccountingPointEnvelopeProvider {

    private final Flux<IdentifiableAccountingPointData> identifiableData;
    private final CommonInformationModelConfiguration cimConfig;
    private final DeEtaPlusConfiguration deConfiguration;

    public DeEtaAccountingPointEnvelopeProvider(
            AccountingPointDataStream stream,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            CommonInformationModelConfiguration cimConfig,
            DeEtaPlusConfiguration deConfiguration
    ) {
        this.identifiableData = stream.accountingPointData();
        this.cimConfig = cimConfig;
        this.deConfiguration = deConfiguration;
    }

    @MessageStream(AccountingPointEnvelope.class)
    public Flux<AccountingPointEnvelope> getAccountingPointMarketDocumentsStream() {
        return identifiableData
                .map(data -> new IntermediateAccountingPointDataMarketDocument(cimConfig, deConfiguration, data))
                .map(IntermediateAccountingPointDataMarketDocument::toEnvelope);
    }
}