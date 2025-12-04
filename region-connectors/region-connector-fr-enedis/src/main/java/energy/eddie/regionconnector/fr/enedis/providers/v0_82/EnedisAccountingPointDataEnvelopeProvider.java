package energy.eddie.regionconnector.fr.enedis.providers.v0_82;

import energy.eddie.api.v0_82.AccountingPointEnvelopeProvider;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableAccountingPointData;
import energy.eddie.regionconnector.fr.enedis.services.EnergyDataStreams;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class EnedisAccountingPointDataEnvelopeProvider implements AccountingPointEnvelopeProvider {

    private final Flux<IdentifiableAccountingPointData> identifiableAccountingPointDataFlux;
    private final IntermediateMarketDocumentFactory intermediateMarketDocumentFactory;

    public EnedisAccountingPointDataEnvelopeProvider(
            EnergyDataStreams streams,
            IntermediateMarketDocumentFactory intermediateMarketDocumentFactory
    ) {
        this.identifiableAccountingPointDataFlux = streams.getAccountingPointData();
        this.intermediateMarketDocumentFactory = intermediateMarketDocumentFactory;
    }

    @Override
    public Flux<AccountingPointEnvelope> getAccountingPointEnvelopeFlux() {
        return identifiableAccountingPointDataFlux
                .map(intermediateMarketDocumentFactory::create)
                .map(IntermediateAccountingPointDataMarketDocument::accountingPointEnvelope);
    }


    @Override
    public void close() throws Exception {
        // No-Op
    }
}
