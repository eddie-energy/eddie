package energy.eddie.regionconnector.us.green.button.providers.v0_82;

import energy.eddie.api.v0_82.AccountingPointEnvelopeProvider;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.regionconnector.us.green.button.services.PublishService;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class AccountingPointDataProvider  implements AccountingPointEnvelopeProvider {
    private final Flux<AccountingPointEnvelope> accountingPointEnvelopeFlux;

    public AccountingPointDataProvider(PublishService publishService, Jaxb2Marshaller marshaller) {
        accountingPointEnvelopeFlux = publishService.accountingPointData()
                .map(id -> new IntermediateAccountingPointMarketDocument(id, marshaller))
                .flatMapIterable(IntermediateAccountingPointMarketDocument::toAps);
    }

    @Override
    public Flux<AccountingPointEnvelope> getAccountingPointEnvelopeFlux() {
        return accountingPointEnvelopeFlux;
    }

    @Override
    public void close() throws Exception {
        // No-Op
    }
}
