// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.connectionlimit;

import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.errors.connectionlimit.ConnectionLimitMonitoringNotAllowedException;
import energy.eddie.aiida.errors.datasource.DataSourceNotFoundException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.models.connectionlimit.ConnectionLimitMonitoring;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.repositories.ConnectionLimitMonitoringRepository;
import energy.eddie.aiida.repositories.DataSourceRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.services.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Manages which data source is monitored for connection limit violations of a permission.
 */
@Service
public class ConnectionLimitMonitoringService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionLimitMonitoringService.class);

    private final ConnectionLimitMonitoringRepository connectionLimitMonitoringRepository;
    private final PermissionRepository permissionRepository;
    private final DataSourceRepository dataSourceRepository;
    private final AuthService authService;

    public ConnectionLimitMonitoringService(
            ConnectionLimitMonitoringRepository connectionLimitMonitoringRepository,
            PermissionRepository permissionRepository,
            DataSourceRepository dataSourceRepository,
            AuthService authService
    ) {
        this.connectionLimitMonitoringRepository = connectionLimitMonitoringRepository;
        this.permissionRepository = permissionRepository;
        this.dataSourceRepository = dataSourceRepository;
        this.authService = authService;
    }

    /**
     * Assigns the given outbound data source for monitoring.
     */
    @Transactional(rollbackFor = Exception.class)
    public ConnectionLimitMonitoring updateConnectionLimitMonitoring(
            UUID permissionId,
            UUID dataSourceId
    ) throws PermissionNotFoundException, InvalidUserException, ConnectionLimitMonitoringNotAllowedException, DataSourceNotFoundException {
        requireMonitorablePermission(permissionId);
        requireMonitorableDataSource(dataSourceId);

        var monitoring = new ConnectionLimitMonitoring(permissionId, dataSourceId);
        connectionLimitMonitoringRepository.save(monitoring);
        LOGGER.debug("Assigned data source {} for connection limit monitoring of permission {}",
                     dataSourceId,
                     permissionId);

        return monitoring;
    }

    /**
     * Clears the monitoring assignment of the given permission.
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteConnectionLimitMonitoring(UUID permissionId) throws PermissionNotFoundException, InvalidUserException, ConnectionLimitMonitoringNotAllowedException {
        requireMonitorablePermission(permissionId);

        connectionLimitMonitoringRepository.deleteById(permissionId);
        LOGGER.debug("Cleared connection limit monitoring of permission {}", permissionId);
    }

    private void requireMonitorablePermission(UUID permissionId) throws PermissionNotFoundException, InvalidUserException, ConnectionLimitMonitoringNotAllowedException {
        var currentUserId = authService.getCurrentUserId();
        var permission = permissionRepository.findByPermissionIdAndUserId(permissionId, currentUserId)
                                             .orElseThrow(() -> new PermissionNotFoundException(permissionId));

        if (!permission.supportsConnectionLimits()) {
            throw new ConnectionLimitMonitoringNotAllowedException(permissionId);
        }
    }

    private void requireMonitorableDataSource(UUID dataSourceId) throws DataSourceNotFoundException, InvalidUserException, ConnectionLimitMonitoringNotAllowedException {
        var currentUserId = authService.getCurrentUserId();
        var dataSource = dataSourceRepository.findByIdAndUserId(dataSourceId, currentUserId)
                                             .orElseThrow(() -> new DataSourceNotFoundException(dataSourceId));

        if (dataSource.type() == DataSourceType.INBOUND) {
            throw new ConnectionLimitMonitoringNotAllowedException(
                    "Data source '%s' is an inbound data source and cannot be monitored for connection limit violations.".formatted(
                            dataSourceId));
        }
    }
}
