package energy.eddie.regionconnector.dk.energinet.providers.v0_82;

import energy.eddie.api.v0_82.EddieAccountingPointMarketDocumentProvider;
import energy.eddie.api.v0_82.cim.EddieAccountingPointMarketDocument;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableAccountingPointDetails;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class EnerginetEddieAccountingPointMarketDocumentProvider implements EddieAccountingPointMarketDocumentProvider {
    private final Flux<EddieAccountingPointMarketDocument> eddieAccountingPointMarketDocumentFlux;
    private final CommonInformationModelConfiguration cimConfiguration;

    public EnerginetEddieAccountingPointMarketDocumentProvider(
            Flux<IdentifiableAccountingPointDetails> identifiableMeteringPointDetailsFlux,
            CommonInformationModelConfiguration cimConfiguration
    ) {
        this.eddieAccountingPointMarketDocumentFlux = identifiableMeteringPointDetailsFlux
                .map(this::mapToAccountingPointMarketDocument)
                .share();
        this.cimConfiguration = cimConfiguration;
    }

    private EddieAccountingPointMarketDocument mapToAccountingPointMarketDocument(
            IdentifiableAccountingPointDetails response
    ) {
        return new EddieAccountingPointMarketDocument(
                response.permissionRequest().connectionId(),
                response.permissionRequest().permissionId(),
                response.permissionRequest().dataNeedId(),
                new IntermediateAccountingPointMarketDocument(response, cimConfiguration)
                        .accountingPointMarketDocument()
        );
    }

    @Override
    public Flux<EddieAccountingPointMarketDocument> getEddieAccountingPointMarketDocumentStream() {
        return eddieAccountingPointMarketDocumentFlux;
    }

    @Override
    public void close() throws Exception {
        // No resources to close
    }
}
