// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.service;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.be.fluvius.client.FluviusApi;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.mandate.GetMandateResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.permission.events.AcceptedEvent;
import energy.eddie.regionconnector.be.fluvius.permission.events.SimpleEvent;
import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest;
import energy.eddie.regionconnector.be.fluvius.permission.request.MeterReading;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class AcceptanceOrRejectionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcceptanceOrRejectionService.class);
    private final BePermissionRequestRepository bePermissionRequestRepository;
    private final FluviusApi fluviusApi;
    private final Outbox outbox;
    private final DataNeedsService dataNeedsService;

    public AcceptanceOrRejectionService(
            BePermissionRequestRepository bePermissionRequestRepository,
            FluviusApi fluviusApi,
            Outbox outbox,
            DataNeedsService dataNeedsService
    ) {
        this.bePermissionRequestRepository = bePermissionRequestRepository;
        this.fluviusApi = fluviusApi;
        this.outbox = outbox;
        this.dataNeedsService = dataNeedsService;
    }

    @Scheduled(cron = "${region-connector.be.fluvius.check-acceptance:0 0 * * * *}")
    public void checkForAcceptance() {
        var permissionRequests = bePermissionRequestRepository.findByStatus(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);
        LOGGER.info("Checking for acceptance permission requests");
        for (var permissionRequest : permissionRequests) {
            var permissionId = permissionRequest.permissionId();
            LOGGER.info("Checking for acceptance of permission request {}", permissionId);
            checkAcceptance(permissionRequest);
        }
    }

    /**
     * Tries to transition the permission request to the desired status.
     * If the permission request is already in the desired state does nothing.
     *
     * @param permissionId  the permissionID of the permission request to be transitioned
     * @param desiredStatus the new status of the permission request, only {@code ACCEPTED} and {@code REJECTED} are allowed
     * @return if the permission request was transitioned to the {@code ACCEPTED} status
     * @throws PermissionNotFoundException if the permissionID does not exist
     */
    public boolean acceptOrRejectPermissionRequest(
            String permissionId,
            PermissionProcessStatus desiredStatus
    ) throws PermissionNotFoundException {
        if (desiredStatus != PermissionProcessStatus.ACCEPTED && desiredStatus != PermissionProcessStatus.REJECTED) {
            throw new IllegalArgumentException();
        }
        var res = bePermissionRequestRepository.findByPermissionId(permissionId);
        if (res.isEmpty()) {
            throw new PermissionNotFoundException(permissionId);
        }
        var pr = res.get();
        permissionId = pr.permissionId();
        var currentStatus = pr.status();
        if (currentStatus != PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR) {
            LOGGER.info("Permission request {} was {} and not {}",
                        permissionId,
                        currentStatus,
                        PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);
            return isAccepted(currentStatus);
        }
        if (isRejected(desiredStatus)) {
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.REJECTED));
            return false;
        }
        checkAcceptance(pr);
        return true;
    }


    private void checkAcceptance(FluviusPermissionRequest permissionRequest) {
        fluviusApi.mandateFor(permissionRequest.permissionId())
                  .subscribe(
                          res -> handleSuccess(res, permissionRequest),
                          error -> LOGGER.warn(
                                  "Unexpected error when requesting status of permission request {}",
                                  permissionRequest.permissionId(),
                                  error
                          )
                  );
    }


    @SuppressWarnings("NullAway") // False positive for the nullable switch expression
    private void handleSuccess(GetMandateResponseModelApiDataResponse res, FluviusPermissionRequest permissionRequest) {
        var permissionId = permissionRequest.permissionId();
        if (res.data() == null || res.data().mandates() == null || res.data().mandates().isEmpty()) {
            LOGGER.info("No mandates found for permission request {}", permissionId);
            return;
        }
        var mandates = res.data().mandates();
        var dn = (ValidatedHistoricalDataDataNeed) dataNeedsService.getById(permissionRequest.dataNeedId());
        var approvedMeters = new ArrayList<String>();
        var rejected = 0;
        for (var mandate : mandates) {
            if (!mandate.supportsGranularity(permissionRequest.granularity()) || !mandate.supportsEnergyType(dn.energyType())) {
                continue;
            }
            var ean = mandate.eanNumber();
            var status = mandate.status();
            switch (status) {
                case APPROVED -> {
                    LOGGER.info("Meter {} of permission request {} approved", ean, permissionId);
                    approvedMeters.add(ean);
                }
                case REJECTED -> {
                    LOGGER.info("Meter {} of permission request {} rejected", ean, permissionId);
                    rejected++;
                }
                case null, default -> LOGGER.warn("Meter {} of permission request {} has unexpected status {}",
                                                  ean,
                                                  permissionId,
                                                  status);
            }
        }
        if (rejected == mandates.size()) {
            LOGGER.info("Permission request {} has been rejected", permissionId);
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.REJECTED));
            return;
        }
        if (approvedMeters.isEmpty()) {
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.ACCEPTED));
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.UNFULFILLABLE));
            return;
        }
        var meters = new ArrayList<MeterReading>();
        for (var approvedMeter : approvedMeters) {
            meters.add(new MeterReading(permissionId, approvedMeter, null));
        }
        outbox.commit(new AcceptedEvent(permissionId, meters));
    }

    private static boolean isAccepted(PermissionProcessStatus status) {
        return status == PermissionProcessStatus.ACCEPTED;
    }

    private static boolean isRejected(PermissionProcessStatus status) {
        return status == PermissionProcessStatus.REJECTED;
    }
}
