package energy.eddie.regionconnector.dk.energinet.providers.v0_82;

import energy.eddie.api.v0_82.AccountingPointEnveloppeProvider;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.ap.AccountingPointEnveloppe;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableAccountingPointDetails;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class EnerginetAccountingPointEnveloppeProvider implements AccountingPointEnveloppeProvider {
    private final Flux<AccountingPointEnveloppe> accountingPointEnveloppeFlux;
    private final CommonInformationModelConfiguration cimConfiguration;

    public EnerginetAccountingPointEnveloppeProvider(
            Flux<IdentifiableAccountingPointDetails> identifiableMeteringPointDetailsFlux,
            CommonInformationModelConfiguration cimConfiguration
    ) {
        this.accountingPointEnveloppeFlux = identifiableMeteringPointDetailsFlux
                .map(this::mapToAccountingPointMarketDocument)
                .share();
        this.cimConfiguration = cimConfiguration;
    }

    private AccountingPointEnveloppe mapToAccountingPointMarketDocument(
            IdentifiableAccountingPointDetails response
    ) {
        return new IntermediateAccountingPointMarketDocument(response, cimConfiguration)
                .accountingPointMarketDocument();
    }

    @Override
    public Flux<AccountingPointEnveloppe> getAccountingPointEnveloppeFlux() {
        return accountingPointEnveloppeFlux;
    }

    @Override
    public void close() throws Exception {
        // No resources to close
    }
}
