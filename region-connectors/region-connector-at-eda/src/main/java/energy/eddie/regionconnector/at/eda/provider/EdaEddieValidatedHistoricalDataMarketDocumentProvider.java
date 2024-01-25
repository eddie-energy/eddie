package energy.eddie.regionconnector.at.eda.provider;

import energy.eddie.api.v0_82.CimConsumptionRecordProvider;
import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.ValidatedHistoricalDataMarketDocumentDirector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;

import java.util.Optional;
import java.util.concurrent.Flow;

import static java.util.Objects.requireNonNull;

/**
 * This class is for processing incoming consumption records by mapping it to ValidatedHistoricalDataMarketDocuments and emitting it for all matching permission requests
 */
public class EdaEddieValidatedHistoricalDataMarketDocumentProvider implements CimConsumptionRecordProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(EdaEddieValidatedHistoricalDataMarketDocumentProvider.class);

    private final ValidatedHistoricalDataMarketDocumentDirector director;
    private final Flux<EddieValidatedHistoricalDataMarketDocument> eddieValidatedHistoricalDataMarketDocumentFlux;

    public EdaEddieValidatedHistoricalDataMarketDocumentProvider(
            ValidatedHistoricalDataMarketDocumentDirector validatedHistoricalDataMarketDocumentDirector,
            Flux<IdentifiableConsumptionRecord> identfiableConsumptionRecordFlux) {
        requireNonNull(validatedHistoricalDataMarketDocumentDirector);
        requireNonNull(identfiableConsumptionRecordFlux);

        this.director = validatedHistoricalDataMarketDocumentDirector;

        this.eddieValidatedHistoricalDataMarketDocumentFlux = identfiableConsumptionRecordFlux
                .flatMap(this::mapToValidatedHistoricalMarketDocument);  // the mapping method is called for each element for each subscriber if we at some point have multiple subscribers, consider using publish().refCount()
    }


    @Override
    public Flow.Publisher<EddieValidatedHistoricalDataMarketDocument> getEddieValidatedHistoricalDataMarketDocumentStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(eddieValidatedHistoricalDataMarketDocumentFlux);
    }

    @Override
    public void close() throws Exception {
        // Nothing to clean up, flux is closed when the underlying flux is closed
    }

    private Flux<EddieValidatedHistoricalDataMarketDocument> mapToValidatedHistoricalMarketDocument(IdentifiableConsumptionRecord identifiableConsumptionRecord) {
        try {
            var marketDocument = director.createValidatedHistoricalDataMarketDocument(identifiableConsumptionRecord.consumptionRecord());

            return Flux.fromIterable(identifiableConsumptionRecord.permissionRequests()).map(permissionRequest -> new EddieValidatedHistoricalDataMarketDocument(
                    Optional.ofNullable(permissionRequest.connectionId()),
                    Optional.ofNullable(permissionRequest.permissionId()),
                    Optional.ofNullable(permissionRequest.dataNeedId()),
                    marketDocument
            ));
        } catch (InvalidMappingException e) {
            LOGGER.error("Error while trying to create ValidatedHistoricalDataMarketDocument from consumption record", e);
            return Flux.empty();
        }
    }
}