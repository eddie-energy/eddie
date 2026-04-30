// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.services.cim.v0_82;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.regionconnector.fi.fingrid.services.EnergyDataService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@SuppressWarnings("java:S6830")
@Component("CimStreamsV0_82")
public class CimStreams {
    private final EnergyDataService energyDataService;

    public CimStreams(EnergyDataService energyDataService) {this.energyDataService = energyDataService;}

    @MessageStream(ValidatedHistoricalDataEnvelope.class)
    public Flux<ValidatedHistoricalDataEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return energyDataService.getIdentifiableValidatedHistoricalDataStream()
                                .flatMapIterable(id -> new IntermediateValidatedHistoricalDataMarketDocument(id.payload(),
                                                                                                             id.permissionRequest()).toVhds());
    }

    @MessageStream(AccountingPointEnvelope.class)
    public Flux<AccountingPointEnvelope> getAccountingPointEnvelopeFlux() {
        return energyDataService.getIdentifiableAccountingPointDataStream()
                                .map(id -> new IntermediateAccountingPointDataMarketDocument(id).toAp());
    }
}
