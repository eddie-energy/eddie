package energy.eddie.aiida.services.monitoring;

import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.monitoring.metrics.DataSourceMetrics;
import energy.eddie.aiida.repositories.AiidaRecordRepository;
import energy.eddie.aiida.repositories.DataSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiidaMonitoringService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiidaMonitoringService.class);
    private final DataSourceRepository dataSourceRepository;
    private final AiidaRecordRepository aiidaRecordRepository;

    @Autowired
    public AiidaMonitoringService(DataSourceRepository dataSourceRepository,AiidaRecordRepository aiidaRecordRepository ) {
        this.dataSourceRepository = dataSourceRepository;
        this.aiidaRecordRepository = aiidaRecordRepository;
    }

    @Scheduled(fixedRate = 50000)
    public List<DataSourceMetrics> getDataSourceMetrics() {
        List<DataSourceMetrics> result = new ArrayList<>();
        var now = Instant.now();

        var dataSources = dataSourceRepository.findAll().stream()
                                              .filter(DataSource::isEnabled)
                                              .toList();

        for (var ds : dataSources) {
            var id = ds.getId();

            long count24h = aiidaRecordRepository.countByDataSourceIdAndTimestampAfter(id, now.minusSeconds(86400));
            long count1h = aiidaRecordRepository.countByDataSourceIdAndTimestampAfter(id, now.minusSeconds(3600));
            long count1min = aiidaRecordRepository.countByDataSourceIdAndTimestampAfter(id, now.minusSeconds(60));

            var lastMsg = aiidaRecordRepository.findTopByDataSourceIdOrderByTimestampDesc(id);
            String lastTimestamp = lastMsg
                    .map(r -> r.timestamp().atOffset(ZoneOffset.UTC).toString())
                    .orElse("");

            var metric = new DataSourceMetrics(
                    now.atOffset(ZoneOffset.UTC).toString(),
                    ds.getDataSourceType().dataSourceName(),
                    ds.getName(),
                    count24h,
                    count1h,
                    count1min,
                    lastTimestamp
            );

            LOGGER.info("Data source {} has {} records in the last 24h, {} in the last 1h, and {} in the last 1min. Last message timestamp: {}",
                    ds.getName(), count24h, count1h, count1min, lastTimestamp);

            result.add(metric);
        }


        return result;
    }
}
