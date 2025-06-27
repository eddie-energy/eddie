package energy.eddie.aiida.services;

import energy.eddie.aiida.errors.UnauthorizedException;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.repositories.DataSourceRepository;
import energy.eddie.aiida.repositories.InboundRecordRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class InboundService {
    private static final Logger LOGGER = LoggerFactory.getLogger(InboundService.class);
    private final DataSourceRepository dataSourceRepository;
    private final InboundRecordRepository inboundRecordRepository;

    public InboundService(DataSourceRepository dataSourceRepository, InboundRecordRepository inboundRecordRepository) {
        this.dataSourceRepository = dataSourceRepository;
        this.inboundRecordRepository = inboundRecordRepository;
    }

    public InboundRecord latestRecord(String accessCode, UUID dataSourceId) throws UnauthorizedException {
        LOGGER.trace("Getting latest raw record for dataSource {}", dataSourceId);

        var dataSource = dataSourceRepository
                .findById(dataSourceId)
                .orElseThrow(() -> new EntityNotFoundException("Datasource not found with ID: " + dataSourceId));

        if (dataSource instanceof InboundDataSource inboundDataSource) {
            if (!BCrypt.checkpw(accessCode, inboundDataSource.accessCode())) {
                throw new UnauthorizedException("Access code does not match for data source with ID: " + dataSourceId);
            }

            return inboundRecordRepository
                    .findTopByDataSourceIdOrderByTimestampDesc(dataSourceId)
                    .orElseThrow(() -> new EntityNotFoundException("No entry found for data source with ID: " + dataSourceId));
        } else {
            throw new EntityNotFoundException("Data source with ID " + dataSourceId + " is not an InboundDataSource");
        }
    }
}
