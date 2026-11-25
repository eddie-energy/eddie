// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.dtos.events.DataSourceDeletionEvent;
import energy.eddie.aiida.dtos.events.InboundPermissionAcceptEvent;
import energy.eddie.aiida.dtos.events.InboundPermissionEndEvent;
import energy.eddie.aiida.errors.SecretLoadingException;
import energy.eddie.aiida.errors.SecretStoringException;
import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.errors.auth.UnauthorizedException;
import energy.eddie.aiida.errors.inbound.MissingMqttStreamingConfigException;
import energy.eddie.aiida.errors.permission.InboundDataSourceInUseException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.repositories.InboundPermissionRepository;
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
    private final ProvisioningService provisioningService;
    private final InboundPermissionRepository inboundPermissionRepository;

    /**
     * Creates the central listener for permission and data-source lifecycle events.
     */
    public AiidaEventListener(
            DataSourceService dataSourceService,
            PermissionService permissionService,
            ProvisioningService provisioningService,
            InboundPermissionRepository inboundPermissionRepository
    ) {
        this.dataSourceService = dataSourceService;
        this.permissionService = permissionService;
        this.provisioningService = provisioningService;
        this.inboundPermissionRepository = inboundPermissionRepository;
    }

    @Transactional
    @EventListener
    protected void createInboundDataSource(InboundPermissionAcceptEvent event) {
        LOGGER.trace("Received InboundPermissionCreationEvent: {}", event);
        var permissionId = event.permissionId();
        try {
            var permission = inboundPermissionRepository.findById(permissionId)
                                                        .orElseThrow(() -> new PermissionNotFoundException(permissionId));
            var inboundDataSource = dataSourceService.createInboundDataSource(permission);

            LOGGER.debug("Set data source {} to permission {}", inboundDataSource, permission);
            permission.setDataSource(inboundDataSource);

            dataSourceService.startDataSource(inboundDataSource);
        } catch (PermissionNotFoundException e) {
            LOGGER.error("No inbound permission found with id {}", permissionId, e);
        } catch (SecretStoringException | SecretLoadingException e) {
            LOGGER.error("Could not create secrets for inbound datasource.", e);
        } catch (MissingMqttStreamingConfigException e) {
            LOGGER.error(
                    "Could not create inbound data source because its permission has no MQTT streaming configuration.",
                    e);
        }
    }

    /**
     * Stops the runtime publisher and deletes the inbound data source of the ended (revoked, terminated or expired)
     * permission. Deleting the entity also removes its persisted provisioning connection, MQTT user credentials, and
     * ACL through JPA cascade removal.
     *
     * @param event End event identifying the inbound data source.
     */
    @EventListener
    protected void deleteInboundDataSource(InboundPermissionEndEvent event) {
        LOGGER.trace("Received InboundPermissionEndEvent: {}", event);
        provisioningService.stopPublisher(event.dataSourceId());
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
            } catch (InboundDataSourceInUseException e) {
                LOGGER.error(
                        "Permission {} associated with deleted data source cannot be revoked, because its inbound data source is still used by outbound permissions.",
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
