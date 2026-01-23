// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.services;

import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.us.green.button.permission.events.UsUnfulfillableEvent;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionRequestService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestService.class);
    private final Outbox outbox;
    private final UsPermissionRequestRepository repository;
    private final EntityManager entityManager;

    public PermissionRequestService(
            Outbox outbox,
            UsPermissionRequestRepository repository,
            EntityManager entityManager
    ) {
        this.outbox = outbox;
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void removeUnfulfillablePermissionRequest(String permissionId) {
        var permissionRequest = repository.getByPermissionId(permissionId);
        entityManager.refresh(permissionRequest);
        if (!permissionRequest.allowedMeters().isEmpty()) {
            return;
        }
        LOGGER.info("Marking permission request {} as unfulfillable, since no meter supports data need",
                    permissionId);
        outbox.commit(new UsUnfulfillableEvent(permissionId, true));
    }

    public List<UsGreenButtonPermissionRequest> findActivePermissionRequests() {
        return List.copyOf(repository.findActivePermissionRequests());
    }
}
