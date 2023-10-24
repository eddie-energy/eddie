package energy.eddie.aiida.aggregator;

import energy.eddie.aiida.datasources.AiidaDataSource;
import energy.eddie.aiida.models.record.AiidaRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class Aggregator implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Aggregator.class);
    private final List<AiidaDataSource> sources;
    private final Sinks.Many<AiidaRecord> combinedSink;

    public Aggregator() {
        sources = new ArrayList<>();
        combinedSink = Sinks.many().multicast().directAllOrNothing();
    }

    /**
     * Adds a new {@link AiidaDataSource} to this aggregator and will subscribe to the Flux returned by {@link AiidaDataSource#start()}.
     *
     * @param dataSource The new datasource to add. No check is made if this is a duplicate.
     */
    public void addNewAiidaDataSource(AiidaDataSource dataSource) {
        LOGGER.info("Will add datasource {} to aggregator", dataSource.name());

        sources.add(dataSource);
        dataSource.start().subscribe(this::publishRecordToCombinedFlux, throwable -> handleError(throwable, dataSource));
    }

    private void handleError(Throwable throwable, AiidaDataSource dataSource) {
        // TODO: do we try to restart the affected datasource or only notify user?
        LOGGER.error("Error from datasource {}", dataSource.name(), throwable);
    }

    private void publishRecordToCombinedFlux(AiidaRecord data) {
        var result = combinedSink.tryEmitNext(data);

        if (result.isFailure())
            LOGGER.error("Error while emitting record to combined sink. Error was: {}", result);
    }

    /**
     * Returns a filtered Flux of {@link AiidaRecord}s that only contains records with a {@link AiidaRecord#code()}
     * that is in the set {@code allowedCodes}.
     *
     * @param allowedCodes Codes which should be included in the returned Flux.
     * @return A Flux on which will only have AiidaRecords with a code matching one of the codes of the allowedCodes set.
     */
    public Flux<AiidaRecord> getFilteredFlux(Set<String> allowedCodes) {
        return combinedSink.asFlux().filter(aiidaRecord -> allowedCodes.contains(aiidaRecord.code()));
    }

    /**
     * Closes all {@link AiidaDataSource}s and emits a complete signal for all the Flux returned by {@link #getFilteredFlux(Set)}.
     */
    @Override
    public void close() {
        // ignore if complete signal can't be emitted successfully
        combinedSink.tryEmitComplete();

        LOGGER.info("Closing all {} datasources", sources.size());
        for (AiidaDataSource source : sources) {
            source.close();
        }
    }
}
