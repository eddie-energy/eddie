// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.aggregator;

import energy.eddie.aiida.adapters.datasource.DataSourceAdapter;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.record.AbstractDataRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractAggregator<T extends AbstractDataRecord> implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAggregator.class);
    private static final String HEALTH_REGISTRY_PREFIX = "DATA_SOURCE_";

    protected final List<DataSourceAdapter<? extends DataSource>> dataSourceAdapters;
    protected final HealthContributorRegistry healthContributorRegistry;
    protected Sinks.Many<T> combinedRecordSink;

    protected AbstractAggregator(
            HealthContributorRegistry healthContributorRegistry
    ) {
        this.healthContributorRegistry = healthContributorRegistry;

        combinedRecordSink = Sinks.many().multicast().directAllOrNothing();
        combinedRecordSink.asFlux()
                          .publishOn(Schedulers.boundedElastic())
                          .doOnNext(this::saveRecordToDatabase)
                          .doOnError(this::handleCombinedSinkError)
                          .subscribe();

        dataSourceAdapters = new ArrayList<>();
    }

    /**
     * Adds a new {@link DataSourceAdapter} to this aggregator and will subscribe to the Flux returned by
     * {@link DataSourceAdapter#start()}.
     *
     * @param dataSourceAdapter The new data source adapter to add. No check is made if this is a duplicate.
     */
    public void addNewDataSourceAdapter(DataSourceAdapter<? extends DataSource> dataSourceAdapter) {
        var dataSource = dataSourceAdapter.dataSource();
        LOGGER.info("Will add datasource {} with ID {} to aggregator", dataSource.name(), dataSource.id());

        healthContributorRegistry.registerContributor(HEALTH_REGISTRY_PREFIX + dataSource.id(), dataSourceAdapter);
        dataSourceAdapters.add(dataSourceAdapter);
    }

    /**
     * Removes a {@link DataSourceAdapter} from this aggregator and will close the datasource.
     *
     * @param dataSourceAdapter The datasource adapter to remove.
     */
    public void removeDataSourceAdapter(DataSourceAdapter<? extends DataSource> dataSourceAdapter) {
        var dataSource = dataSourceAdapter.dataSource();
        LOGGER.info("Will remove datasource {} with ID {} from aggregator", dataSource.name(), dataSource.id());

        healthContributorRegistry.unregisterContributor(HEALTH_REGISTRY_PREFIX + dataSource.id());
        dataSourceAdapters.remove(dataSourceAdapter);
        dataSourceAdapter.close();
    }

    /**
     * Closes all {@link energy.eddie.aiida.adapters.datasource.inbound.InboundAdapter}s and emits a complete signal for all the Flux returned by
     */
    @Override
    public void close() {
        // ignore if complete signal can't be emitted successfully
        combinedRecordSink.tryEmitComplete();

        LOGGER.info("Closing all {} datasources", dataSourceAdapters.size());
        for (var dataSourceAdapter : dataSourceAdapters) {
            dataSourceAdapter.close();
        }
    }

    protected abstract void saveRecordToDatabase(T dataRecord);

    protected synchronized void publishRecordToCombinedFlux(T data) {
        var result = combinedRecordSink.tryEmitNext(data);

        if (result.isFailure()) {
            LOGGER.error("Error while emitting record to combined sink. Error was: {}", result);
        }
    }

    protected void handleCombinedSinkError(Throwable throwable) {
        LOGGER.error("Error from combined sink", throwable);
    }

    protected void handleError(Throwable throwable, DataSourceAdapter<? extends DataSource> dataSourceAdapter) {
        // TODO: GH-1591 do we try to restart the affected datasource or only notify user?
        LOGGER.error("Error from datasource {}", dataSourceAdapter.dataSource().name(), throwable);
    }
}
