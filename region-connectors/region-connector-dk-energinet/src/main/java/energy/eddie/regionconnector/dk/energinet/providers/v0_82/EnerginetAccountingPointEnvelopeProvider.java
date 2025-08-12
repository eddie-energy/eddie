package energy.eddie.regionconnector.dk.energinet.providers.v0_82;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0_82.AccountingPointEnvelopeProvider;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableAccountingPointDetails;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class EnerginetAccountingPointEnvelopeProvider implements AccountingPointEnvelopeProvider {
    private final Flux<AccountingPointEnvelope> accountingPointEnvelopeFlux;
    private final CommonInformationModelConfiguration cimConfiguration;

    public EnerginetAccountingPointEnvelopeProvider(
            Flux<IdentifiableAccountingPointDetails> identifiableMeteringPointDetailsFlux,
            CommonInformationModelConfiguration cimConfiguration
    ) {
        this.accountingPointEnvelopeFlux = identifiableMeteringPointDetailsFlux
                .map(this::mapToAccountingPointMarketDocument)
                .share();
        this.cimConfiguration = cimConfiguration;
    }

    private AccountingPointEnvelope mapToAccountingPointMarketDocument(
            IdentifiableAccountingPointDetails response
    ) {
        return new IntermediateAccountingPointMarketDocument(response, cimConfiguration)
                .accountingPointMarketDocument();
    }

    @Override
    public Flux<AccountingPointEnvelope> getAccountingPointEnvelopeFlux() {
        return accountingPointEnvelopeFlux;
    }

    @Override
    public void close() throws Exception {
        // No resources to close
    }
}
