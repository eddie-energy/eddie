package energy.eddie.aiida.services;

import energy.eddie.aiida.errors.PermissionNotFoundException;
import energy.eddie.aiida.errors.UnauthorizedException;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.repositories.InboundRecordRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class InboundService {
    private static final Logger LOGGER = LoggerFactory.getLogger(InboundService.class);
    private final InboundRecordRepository inboundRecordRepository;
    private final PermissionRepository permissionRepository;

    public InboundService(
            InboundRecordRepository inboundRecordRepository,
            PermissionRepository permissionRepository
    ) {
        this.inboundRecordRepository = inboundRecordRepository;
        this.permissionRepository = permissionRepository;
    }

    public InboundRecord latestRecord(String accessCode, UUID permissionId) throws PermissionNotFoundException, UnauthorizedException {
        LOGGER.trace("Getting latest raw record for permission {}", permissionId);

        var permission = permissionRepository
                .findById(permissionId)
                .orElseThrow(() -> new PermissionNotFoundException(permissionId));
        var dataSource = permission.dataSource();

        if (!(dataSource instanceof InboundDataSource inboundDataSource)) {
            throw new EntityNotFoundException("Data source is not an InboundDataSource");
        }

        if (!accessCode.equals(inboundDataSource.accessCode())) {
            throw new UnauthorizedException("Access code does not match for data source with ID: " + dataSource.id());
        }

        return inboundRecordRepository
                .findTopByDataSourceIdOrderByTimestampDesc(dataSource.id())
                .orElseThrow(() -> new EntityNotFoundException("No entry found for data source with ID: " + dataSource.id()));
    }
}
