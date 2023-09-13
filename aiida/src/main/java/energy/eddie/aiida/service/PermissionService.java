package energy.eddie.aiida.service;

import energy.eddie.aiida.dto.PermissionDto;
import energy.eddie.aiida.error.InvalidPermissionRevocationException;
import energy.eddie.aiida.error.PermissionNotFoundException;
import energy.eddie.aiida.model.permission.Permission;
import energy.eddie.aiida.model.permission.PermissionStatus;
import energy.eddie.aiida.repository.PermissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;

@Service
public class PermissionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionService.class);

    private final PermissionRepository repository;
    private final Clock clock;

    @Autowired
    public PermissionService(PermissionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Permission setupNewPermission(PermissionDto dto) {
        Permission newPermission = new Permission(dto.serviceName(), dto.startTime(), dto.expirationTime(),
                dto.grantTime(), dto.connectionId(), dto.requestedCodes(), dto.kafkaStreamingConfig());
        return repository.save(newPermission);
    }

    public List<Permission> getAllPermissionsSortedByGrantTime() {
        return repository.findAllByOrderByGrantTimeDesc();
    }

    public Permission findById(String permissionId) throws PermissionNotFoundException {
        return repository.findById(permissionId).orElseThrow(() -> new PermissionNotFoundException(permissionId));
    }

    public Permission revokePermission(String permissionId) throws PermissionNotFoundException {
        LOGGER.info("Got request to revoke permission with id {}", permissionId);

        var permission = findById(permissionId);

        if (!isEligibleForRevocation(permission))
            throw new InvalidPermissionRevocationException(permissionId);

        permission.updateStatus(PermissionStatus.REVOKED);
        permission.revokeTime(clock.instant());
        return repository.save(permission);
    }

    private boolean isEligibleForRevocation(Permission permission) {
        return switch (permission.status()) {
            case ACCEPTED, WAITING_FOR_START, STREAMING_DATA -> true;
            default -> false;
        };
    }
}
