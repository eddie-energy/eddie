package energy.eddie.aiida.services;

import energy.eddie.aiida.errors.SecretLoadingException;
import energy.eddie.aiida.errors.auth.UnauthorizedException;
import energy.eddie.aiida.errors.datasource.InvalidDataSourceTypeException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.errors.record.InboundRecordNotFoundException;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.aiida.repositories.InboundRecordRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.services.secrets.SecretsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
public class InboundService {
    private static final Logger LOGGER = LoggerFactory.getLogger(InboundService.class);

    private final InboundRecordRepository inboundRecordRepository;
    private final PermissionRepository permissionRepository;
    private final SecretsService secretsService;

    public InboundService(
            InboundRecordRepository inboundRecordRepository,
            PermissionRepository permissionRepository,
            SecretsService secretsService
    ) {
        this.inboundRecordRepository = inboundRecordRepository;
        this.permissionRepository = permissionRepository;
        this.secretsService = secretsService;
    }

    public InboundRecord latestRecord(
            UUID permissionId,
            String accessCode
    ) throws PermissionNotFoundException, UnauthorizedException,
             InvalidDataSourceTypeException, InboundRecordNotFoundException, SecretLoadingException {
        var dataSource = dataSource(permissionId);
        var savedAccessCode = secretsService.loadSecret(dataSource.accessCode());

        if (!Objects.equals(accessCode, savedAccessCode)) {
            throw new UnauthorizedException(
                    "Access code does not match for data source with ID: " + dataSource.id()
            );
        }

        return latestRecord(dataSource);
    }

    public InboundRecord latestRecord(UUID permissionId)
            throws PermissionNotFoundException, InvalidDataSourceTypeException, InboundRecordNotFoundException {
        return latestRecord(dataSource(permissionId));
    }

    private InboundDataSource dataSource(UUID permissionId)
            throws PermissionNotFoundException, InvalidDataSourceTypeException {
        LOGGER.trace("Getting latest raw record for permission {}", permissionId);

        var permission = permissionRepository.findById(permissionId)
                                             .orElseThrow(() -> new PermissionNotFoundException(permissionId));

        var dataSource = permission.dataSource();
        if (!(dataSource instanceof InboundDataSource inboundDataSource)) {
            throw new InvalidDataSourceTypeException();
        }
        return inboundDataSource;
    }

    private InboundRecord latestRecord(InboundDataSource dataSource) throws InboundRecordNotFoundException {
        return inboundRecordRepository
                .findTopByDataSourceIdOrderByTimestampDesc(dataSource.id())
                .orElseThrow(() -> new InboundRecordNotFoundException(dataSource.id()));
    }
}
