// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.providers.v0_82;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.regionconnector.es.datadis.providers.EnergyDataStreams;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableAccountingPointData;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class DatadisAccountingPointEnvelopeProvider {

    private final Flux<IdentifiableAccountingPointData> identifiableMeterReadings;
    private final IntermediateAPMDFactory intermediateAPMDFactory;

    public DatadisAccountingPointEnvelopeProvider(
            EnergyDataStreams streams,
            IntermediateAPMDFactory intermediateAPMDFactory
    ) {
        this.identifiableMeterReadings = streams.getAccountingPointData();
        this.intermediateAPMDFactory = intermediateAPMDFactory;
    }

    @MessageStream(AccountingPointEnvelope.class)
    public Flux<AccountingPointEnvelope> getAccountingPointEnvelopeFlux() {
        return identifiableMeterReadings
                .map(intermediateAPMDFactory::create)
                .map(IntermediateAccountingPointMarketDocument::accountingPointEnvelope);
    }
}
