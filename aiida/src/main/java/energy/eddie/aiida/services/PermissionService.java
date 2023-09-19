package energy.eddie.aiida.services;

import energy.eddie.aiida.dtos.PermissionDto;
import energy.eddie.aiida.errors.InvalidPermissionRevocationException;
import energy.eddie.aiida.errors.PermissionNotFoundException;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.repositories.PermissionRepository;
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

    /**
     * Saves a new permission in the database.
     *
     * @param dto Data transfer object containing the information for the new permission.
     * @return Permission object as returned by the database (i.e. with a permissionId).
     */
    public Permission setupNewPermission(PermissionDto dto) {
        Permission newPermission = new Permission(dto.serviceName(), dto.startTime(), dto.expirationTime(),
                dto.grantTime(), dto.connectionId(), dto.requestedCodes(), dto.kafkaStreamingConfig());
        return repository.save(newPermission);
    }

    /**
     * Returns all permission objects that are persisted, sorted by their grantTime descending.
     *
     * @return A list of permissions, sorted by grantTime descending, i.e. the permission with the newest grantTime is the first item.
     */
    public List<Permission> getAllPermissionsSortedByGrantTime() {
        return repository.findAllByOrderByGrantTimeDesc();
    }

    /**
     * Returns the permission with the specified ID.
     *
     * @param permissionId ID of the permission to be returned.
     * @return The permission object with the specified ID.
     * @throws PermissionNotFoundException In case no permission with the specified ID can be found.
     */
    public Permission findById(String permissionId) throws PermissionNotFoundException {
        return repository.findById(permissionId).orElseThrow(() -> new PermissionNotFoundException(permissionId));
    }

    /**
     * Revokes the specified permission by updating its status and records the timestamp. Persists the changes.
     *
     * @param permissionId ID of the permission that should be revoked.
     * @return Updated permission object that has been persisted.
     * @throws PermissionNotFoundException          In case no permission with the specified ID can be found.
     * @throws InvalidPermissionRevocationException In case the permission has a status that makes it not eligible for revocation.
     */
    public Permission revokePermission(String permissionId) throws PermissionNotFoundException, InvalidPermissionRevocationException {
        LOGGER.info("Got request to revoke permission with id {}", permissionId);

        var permission = findById(permissionId);

        if (!isEligibleForRevocation(permission))
            throw new InvalidPermissionRevocationException(permissionId);

        permission.updateStatus(PermissionStatus.REVOKED);
        permission.revokeTime(clock.instant());
        return repository.save(permission);
    }

    /**
     * Indicates whether the permission's current status allows it to be revoked.
     * The status needs to be one of the following, to be eligible for revocation: ACCEPTED, WAITING_FOR_START, STREAMING_DATA
     *
     * @param permission Permission to check.
     * @return True if the permission is eligible for revocation, false otherwise.
     */
    private boolean isEligibleForRevocation(Permission permission) {
        return switch (permission.status()) {
            case ACCEPTED, WAITING_FOR_START, STREAMING_DATA -> true;
            default -> false;
        };
    }
}
