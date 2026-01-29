// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.dtos.events.DataSourceDeletionEvent;
import energy.eddie.aiida.dtos.events.InboundPermissionAcceptEvent;
import energy.eddie.aiida.dtos.events.InboundPermissionRevokeEvent;
import energy.eddie.aiida.dtos.events.OutboundPermissionAcceptEvent;
import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.errors.auth.UnauthorizedException;
import energy.eddie.aiida.errors.datasource.DataSourceNotFoundException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AiidaEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiidaEventListener.class);
    private final DataSourceService dataSourceService;
    private final PermissionService permissionService;

    public AiidaEventListener(DataSourceService dataSourceService, PermissionService permissionService) {
        this.dataSourceService = dataSourceService;
        this.permissionService = permissionService;
    }

    @EventListener
    protected void addDataSourceToPermission(OutboundPermissionAcceptEvent event) {
        LOGGER.trace("Received OutboundPermissionCreationEvent: {}", event);
        try {
            var dataSource = dataSourceService.dataSourceByIdOrThrow(event.dataSourceId());
            permissionService.addDataSourceToPermission(dataSource, event.permissionId());
        } catch (DataSourceNotFoundException e) {
            LOGGER.error("No data source found with id {}", event.dataSourceId(), e);
        }
    }

    @Transactional
    @EventListener
    protected void createInboundDataSource(InboundPermissionAcceptEvent event) {
        LOGGER.trace("Received InboundPermissionCreationEvent: {}", event);
        try {
            var permission = permissionService.findById(event.permissionId());
            var inboundDataSource = dataSourceService.createInboundDataSource(permission);

            LOGGER.debug("Set data source {} to permission {}", inboundDataSource, permission);
            permission.setDataSource(inboundDataSource);

            dataSourceService.startDataSource(inboundDataSource);
        } catch (PermissionNotFoundException e) {
            LOGGER.error("No permission found with id {}", event.permissionId(), e);
        }
    }

    @EventListener
    protected void deleteInboundDataSource(InboundPermissionRevokeEvent event) {
        LOGGER.trace("Received InboundPermissionDeletionEvent: {}", event);
        dataSourceService.deleteDataSource(event.dataSourceId());
    }

    @EventListener
    @Transactional
    protected void revokeAllAssociatedPermissions(DataSourceDeletionEvent event) {
        LOGGER.trace("Received DataSourceDeletionEvent: {}", event);
        var revocationCount = 0;

        for (var permissionId : event.permissionIds()) {
            try {
                permissionService.revokePermission(permissionId);
                revocationCount++;
            } catch (PermissionNotFoundException e) {
                LOGGER.warn(
                        "Permission {} associated with deleted data source not found in database anymore, skipping revocation.",
                        permissionId);
            } catch (PermissionStateTransitionException e) {
                LOGGER.error(
                        "Permission {} associated with deleted data source cannot be revoked, because it is not in an eligible state.",
                        permissionId);
            } catch (UnauthorizedException | InvalidUserException e) {
                LOGGER.error(
                        "Permission {} associated with deleted data source cannot be revoked, because the current user is not authorized to do so.",
                        permissionId);
            }
        }

        LOGGER.info("Revoked {} permission(s).", revocationCount);
    }
}
