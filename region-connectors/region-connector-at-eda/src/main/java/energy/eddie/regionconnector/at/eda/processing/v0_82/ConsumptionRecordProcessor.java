package energy.eddie.regionconnector.at.eda.processing.v0_82;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ConsumptionRecord;
import energy.eddie.api.v0_82.CimConsumptionRecordProvider;
import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;
import energy.eddie.cim.validated_historical_data.v0_82.ValidatedHistoricalDataMarketDocument;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
import energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.EddieValidatedHistoricalDataMarketDocumentPublisher;
import energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.ValidatedHistoricalDataMarketDocumentDirector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Mono;

import java.util.concurrent.Flow;

import static java.util.Objects.requireNonNull;

/**
 * This class is for processing incoming consumption records by mapping it to ValidatedHistoricalDataMarketDocuments and emitting it for all matching permission requests
 */
public class ConsumptionRecordProcessor implements CimConsumptionRecordProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionRecordProcessor.class);
    private final EdaAdapter edaAdapter;

    private final ValidatedHistoricalDataMarketDocumentDirector director;
    private final EddieValidatedHistoricalDataMarketDocumentPublisher publisher;

    public ConsumptionRecordProcessor(
            ValidatedHistoricalDataMarketDocumentDirector validatedHistoricalDataMarketDocumentDirector,
            EddieValidatedHistoricalDataMarketDocumentPublisher publisher,
            EdaAdapter edaAdapter) {
        requireNonNull(validatedHistoricalDataMarketDocumentDirector);
        requireNonNull(publisher);
        requireNonNull(edaAdapter);

        this.director = validatedHistoricalDataMarketDocumentDirector;
        this.publisher = publisher;
        this.edaAdapter = edaAdapter;
    }


    @Override
    public Flow.Publisher<EddieValidatedHistoricalDataMarketDocument> getEddieValidatedHistoricalDataMarketDocumentStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(edaAdapter.getConsumptionRecordStream()
                .flatMap(this::mapToValidatedHistoricalMarketDocument)
                .flatMap(publisher::emitForEachPermissionRequest));
    }

    /**
     * Emit a complete signal on the Flow in this method.
     */
    @Override
    public void close() throws Exception {
        edaAdapter.close();
    }

    private Mono<ValidatedHistoricalDataMarketDocument> mapToValidatedHistoricalMarketDocument(ConsumptionRecord consumptionRecord) {
        try {
            return Mono.just(director.createValidatedHistoricalDataMarketDocument(consumptionRecord));
        } catch (InvalidMappingException e) {
            LOGGER.error("Error while trying to create ValidatedHistoricalDataMarketDocument from consumption record", e);
            return Mono.empty();
        }
    }
}