package energy.eddie.regionconnector.at.eda.provider.v0_82;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.v0_82.ValidatedHistoricalDataEnvelopeProvider;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.ValidatedHistoricalDataMarketDocumentDirector;
import energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder.ValidatedHistoricalDataMarketDocumentBuilderFactory;
import energy.eddie.regionconnector.at.eda.provider.IdentifiableStreams;
import energy.eddie.regionconnector.shared.cim.v0_82.vhd.VhdEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import static java.util.Objects.requireNonNull;

/**
 * This class is for processing incoming consumption records by mapping it to ValidatedHistoricalDataMarketDocuments and
 * emitting it for all matching permission requests
 */
@Component
public class EdaValidatedHistoricalDataEnvelopeProvider implements ValidatedHistoricalDataEnvelopeProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(EdaValidatedHistoricalDataEnvelopeProvider.class);

    private final ValidatedHistoricalDataMarketDocumentDirector director;
    private final Flux<ValidatedHistoricalDataEnvelope> eddieValidatedHistoricalDataMarketDocumentFlux;

    @Autowired
    public EdaValidatedHistoricalDataEnvelopeProvider(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") CommonInformationModelConfiguration cimConfig,
            IdentifiableStreams streams
    ) {
        this(
                new ValidatedHistoricalDataMarketDocumentDirector(
                        cimConfig,
                        new ValidatedHistoricalDataMarketDocumentBuilderFactory()
                ),
                streams.consumptionRecordStream()
        );
    }

    EdaValidatedHistoricalDataEnvelopeProvider(
            ValidatedHistoricalDataMarketDocumentDirector validatedHistoricalDataMarketDocumentDirector,
            Flux<IdentifiableConsumptionRecord> identifiableConsumptionRecordFlux
    ) {
        requireNonNull(validatedHistoricalDataMarketDocumentDirector);
        requireNonNull(identifiableConsumptionRecordFlux);

        this.director = validatedHistoricalDataMarketDocumentDirector;

        this.eddieValidatedHistoricalDataMarketDocumentFlux = identifiableConsumptionRecordFlux
                .flatMap(this::mapToValidatedHistoricalMarketDocument);  // the mapping method is called for each element for each subscriber if we at some point have multiple subscribers, consider using publish().refCount()
    }

    @Override
    public Flux<ValidatedHistoricalDataEnvelope> getValidatedHistoricalDataMarketDocumentsStream() {
        return eddieValidatedHistoricalDataMarketDocumentFlux;
    }

    @Override
    public void close() throws Exception {
        // Nothing to clean up, flux is closed when the underlying flux is closed
    }

    private Flux<ValidatedHistoricalDataEnvelope> mapToValidatedHistoricalMarketDocument(
            IdentifiableConsumptionRecord identifiableConsumptionRecord
    ) {
        LOGGER.debug("Mapping validated historical data market document");
        try {
            var marketDocument = director.createValidatedHistoricalDataMarketDocument(identifiableConsumptionRecord.consumptionRecord());

            return Flux.fromIterable(identifiableConsumptionRecord.permissionRequests())
                       .map(permissionRequest -> new VhdEnvelope(marketDocument, permissionRequest))
                       .map(VhdEnvelope::wrap);
        } catch (InvalidMappingException e) {
            LOGGER.error("Error while trying to create ValidatedHistoricalDataMarketDocument from consumption record",
                         e);
            return Flux.empty();
        }
    }
}