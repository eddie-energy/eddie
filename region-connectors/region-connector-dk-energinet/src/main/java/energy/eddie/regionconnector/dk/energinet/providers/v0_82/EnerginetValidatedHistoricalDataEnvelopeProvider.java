package energy.eddie.regionconnector.dk.energinet.providers.v0_82;

import energy.eddie.api.v0_82.ValidatedHistoricalDataEnvelopeProvider;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import energy.eddie.regionconnector.dk.energinet.providers.v0_82.builder.ValidatedHistoricalDataMarketDocumentBuilderFactory;
import energy.eddie.regionconnector.shared.cim.v0_82.vhd.VhdEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Objects;

@Component
public class EnerginetValidatedHistoricalDataEnvelopeProvider implements ValidatedHistoricalDataEnvelopeProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            EnerginetValidatedHistoricalDataEnvelopeProvider.class);
    private final Flux<ValidatedHistoricalDataEnvelope> eddieValidatedHistoricalDataMarketDocumentFlux;
    private final ValidatedHistoricalDataMarketDocumentBuilderFactory validatedHistoricalDataMarketDocumentBuilderFactory;

    public EnerginetValidatedHistoricalDataEnvelopeProvider(
            Flux<IdentifiableApiResponse> identifiableApiResponseFlux,
            ValidatedHistoricalDataMarketDocumentBuilderFactory validatedHistoricalDataMarketDocumentBuilderFactory
    ) {
        this.eddieValidatedHistoricalDataMarketDocumentFlux = identifiableApiResponseFlux
                .flatMap(this::mapToValidatedHistoricalMarketDocument).
                share();
        this.validatedHistoricalDataMarketDocumentBuilderFactory = validatedHistoricalDataMarketDocumentBuilderFactory;
    }

    private Flux<ValidatedHistoricalDataEnvelope> mapToValidatedHistoricalMarketDocument(
            IdentifiableApiResponse response
    ) {
        try {
            var responseMarketDocument = Objects.requireNonNull(response.apiResponse().getMyEnergyDataMarketDocument());
            var validatedHistoricalDataMarketDocument = validatedHistoricalDataMarketDocumentBuilderFactory
                    .create()
                    .withMyEnergyDataMarketDocument(responseMarketDocument)
                    .build();

            return Flux.just(
                    new VhdEnvelope(validatedHistoricalDataMarketDocument, response.permissionRequest()).wrap()
            );
        } catch (Exception e) {
            LOGGER.error("Failed to map to validated historical market document", e);
            return Flux.empty();
        }
    }

    @Override
    public Flux<ValidatedHistoricalDataEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return eddieValidatedHistoricalDataMarketDocumentFlux;
    }

    @Override
    public void close() throws Exception {
        // No resources to close
    }
}
