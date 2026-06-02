// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.aggregator;

import energy.eddie.aiida.adapters.datasource.DataSourceAdapter;
import energy.eddie.aiida.adapters.datasource.inbound.InboundAdapter;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.repositories.InboundRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class InboundAggregator extends AbstractAggregator<InboundRecord> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InboundAggregator.class);

    private final InboundRecordRepository inboundRecordRepository;

    public InboundAggregator(
            InboundRecordRepository inboundRecordRepository,
            HealthContributorRegistry healthContributorRegistry
    ) {
        super(healthContributorRegistry);
        this.inboundRecordRepository = inboundRecordRepository;
    }

    @Override
    public void addNewDataSourceAdapter(DataSourceAdapter<? extends DataSource> dataSourceAdapter) {
        super.addNewDataSourceAdapter(dataSourceAdapter);

        if (!(dataSourceAdapter instanceof InboundAdapter inboundAdapter)) {
            LOGGER.error("Data source adapter of data source {} is not of type {}",
                         dataSourceAdapter.dataSource().name(),
                         InboundAdapter.class.getName());

            return;
        }

        inboundAdapter.inboundRecordFlux()
                      .subscribe(this::saveRecordToDatabase);
    }

    @Override
    protected void saveRecordToDatabase(InboundRecord dataRecord) {
        LOGGER.trace("Saving new inbound record to db");
        inboundRecordRepository.save(dataRecord);
    }

    public Flux<InboundRecord> getInboundRecordFlux() {
        return combinedRecordSink.asFlux();
    }
}
