package energy.eddie.regionconnector.dk.energinet.providers.v0_82;

import energy.eddie.api.v0_82.EddieValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import energy.eddie.regionconnector.dk.energinet.providers.v0_82.builder.ValidatedHistoricalDataMarketDocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Objects;
import java.util.Optional;

@Component
public class EnerginetEddieValidatedHistoricalDataMarketDocumentProvider implements EddieValidatedHistoricalDataMarketDocumentProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnerginetEddieValidatedHistoricalDataMarketDocumentProvider.class);
    private final Flux<EddieValidatedHistoricalDataMarketDocument> eddieValidatedHistoricalDataMarketDocumentFlux;
    private final ValidatedHistoricalDataMarketDocumentBuilderFactory validatedHistoricalDataMarketDocumentBuilderFactory;

    public EnerginetEddieValidatedHistoricalDataMarketDocumentProvider(Flux<IdentifiableApiResponse> identifiableApiResponseFlux, ValidatedHistoricalDataMarketDocumentBuilderFactory validatedHistoricalDataMarketDocumentBuilderFactory) {
        this.eddieValidatedHistoricalDataMarketDocumentFlux = identifiableApiResponseFlux
                .flatMap(this::mapToValidatedHistoricalMarketDocument).
                share();
        this.validatedHistoricalDataMarketDocumentBuilderFactory = validatedHistoricalDataMarketDocumentBuilderFactory;
    }

    private Flux<EddieValidatedHistoricalDataMarketDocument> mapToValidatedHistoricalMarketDocument(IdentifiableApiResponse response) {
        try {
            var responseMarketDocument = Objects.requireNonNull(response.apiResponse().getMyEnergyDataMarketDocument());
            var validatedHistoricalDataMarketDocument = validatedHistoricalDataMarketDocumentBuilderFactory
                    .create()
                    .withMyEnergyDataMarketDocument(responseMarketDocument)
                    .build();

            return Flux.just(
                    new EddieValidatedHistoricalDataMarketDocument(
                            Optional.of(response.connectionId()),
                            Optional.of(response.permissionId()),
                            Optional.of(response.dataNeedId()),
                            validatedHistoricalDataMarketDocument)
            );
        } catch (Exception e) {
            LOGGER.error("Failed to map to validated historical market document", e);
            return Flux.empty();
        }
    }

    @Override
    public Flux<EddieValidatedHistoricalDataMarketDocument> getEddieValidatedHistoricalDataMarketDocumentStream() {
        return eddieValidatedHistoricalDataMarketDocumentFlux;
    }

    @Override
    public void close() throws Exception {
        // No resources to close
    }
}