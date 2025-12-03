package energy.eddie.regionconnector.fi.fingrid.services.cim.v0_82;

import energy.eddie.api.v0_82.AccountingPointEnvelopeProvider;
import energy.eddie.api.v0_82.ValidatedHistoricalDataEnvelopeProvider;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.regionconnector.fi.fingrid.services.EnergyDataService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@SuppressWarnings("java:S6830")
@Component("CimStreamsV0_82")
public class CimStreams implements ValidatedHistoricalDataEnvelopeProvider, AccountingPointEnvelopeProvider {
    private final EnergyDataService energyDataService;

    public CimStreams(EnergyDataService energyDataService) {this.energyDataService = energyDataService;}

    @Override
    public Flux<ValidatedHistoricalDataEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return energyDataService.getIdentifiableValidatedHistoricalDataStream()
                                .flatMapIterable(id -> new IntermediateValidatedHistoricalDataMarketDocument(id.payload(),
                                                                                                             id.permissionRequest()).toVhds());
    }

    @Override
    public void close() throws Exception {
        // No-Op
    }

    @Override
    public Flux<AccountingPointEnvelope> getAccountingPointEnvelopeFlux() {
        return energyDataService.getIdentifiableAccountingPointDataStream()
                                .map(id -> new IntermediateAccountingPointDataMarketDocument(id).toAp());
    }
}
