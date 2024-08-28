package energy.eddie.regionconnector.es.datadis.providers.v0_82;

import energy.eddie.api.v0_82.AccountingPointEnveloppeProvider;
import energy.eddie.cim.v0_82.ap.AccountingPointEnveloppe;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableAccountingPointData;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class DatadisAccountingPointEnveloppeProvider implements AccountingPointEnveloppeProvider {

    private final Flux<IdentifiableAccountingPointData> identifiableMeterReadings;
    private final IntermediateAPMDFactory intermediateAPMDFactory;

    public DatadisAccountingPointEnveloppeProvider(
            Flux<IdentifiableAccountingPointData> identifiableMeterReadings,
            IntermediateAPMDFactory intermediateAPMDFactory
    ) {
        this.identifiableMeterReadings = identifiableMeterReadings;
        this.intermediateAPMDFactory = intermediateAPMDFactory;
    }

    @Override
    public Flux<AccountingPointEnveloppe> getAccountingPointEnveloppeFlux() {
        return identifiableMeterReadings
                .map(intermediateAPMDFactory::create)
                .map(IntermediateAccountingPointMarketDocument::accountingPointEnveloppe);
    }

    @Override
    public void close() throws Exception {
        // No-Op
    }
}
