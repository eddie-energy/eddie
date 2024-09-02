package energy.eddie.regionconnector.es.datadis.providers.v0_82;

import energy.eddie.api.v0_82.AccountingPointEnvelopeProvider;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableAccountingPointData;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class DatadisAccountingPointEnvelopeProvider implements AccountingPointEnvelopeProvider {

    private final Flux<IdentifiableAccountingPointData> identifiableMeterReadings;
    private final IntermediateAPMDFactory intermediateAPMDFactory;

    public DatadisAccountingPointEnvelopeProvider(
            Flux<IdentifiableAccountingPointData> identifiableMeterReadings,
            IntermediateAPMDFactory intermediateAPMDFactory
    ) {
        this.identifiableMeterReadings = identifiableMeterReadings;
        this.intermediateAPMDFactory = intermediateAPMDFactory;
    }

    @Override
    public Flux<AccountingPointEnvelope> getAccountingPointEnvelopeFlux() {
        return identifiableMeterReadings
                .map(intermediateAPMDFactory::create)
                .map(IntermediateAccountingPointMarketDocument::accountingPointEnvelope);
    }

    @Override
    public void close() throws Exception {
        // No-Op
    }
}
