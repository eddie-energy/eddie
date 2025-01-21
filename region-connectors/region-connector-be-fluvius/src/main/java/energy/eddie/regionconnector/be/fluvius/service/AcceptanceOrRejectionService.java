package energy.eddie.regionconnector.be.fluvius.service;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.be.fluvius.client.model.GetMandateResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.clients.FluviusApi;
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

    public AcceptanceOrRejectionService(
            BePermissionRequestRepository bePermissionRequestRepository,
            FluviusApi fluviusApi,
            Outbox outbox
    ) {
        this.bePermissionRequestRepository = bePermissionRequestRepository;
        this.fluviusApi = fluviusApi;
        this.outbox = outbox;
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


    private void handleSuccess(GetMandateResponseModelApiDataResponse res, FluviusPermissionRequest permissionRequest) {
        var permissionId = permissionRequest.permissionId();
        if (res.getData() == null || res.getData().getMandates() == null || res.getData().getMandates().isEmpty()) {
            LOGGER.info("No mandates found for permission request {}", permissionId);
            return;
        }
        var mandates = res.getData().getMandates();
        var approvedMeters = new ArrayList<String>();
        var others = 0;
        var rejected = 0;
        for (var mandate : mandates) {
            var ean = mandate.getEanNumber();
            var status = mandate.getStatus();
            switch (status) {
                case "Approved" -> {
                    LOGGER.info("Meter {} of permission request {} approved", ean, permissionId);
                    approvedMeters.add(ean);
                }
                case "Rejected" -> {
                    LOGGER.info("Meter {} of permission request {} rejected", ean, permissionId);
                    rejected++;
                }
                case "Requested" -> {
                    LOGGER.info("Status of meter {} of permission request {} has not changed", ean, permissionId);
                    others++;
                }
                case null, default -> {
                    LOGGER.warn("Meter {} of permission request {} has unexpected status {}",
                                ean,
                                permissionId,
                                status);
                    others++;
                }
            }
        }
        if (others == mandates.size()) {
            LOGGER.info("Permission request {} not accepted yet", permissionId);
            return;
        }
        if (rejected == mandates.size()) {
            LOGGER.info("Permission request {} has been rejected", permissionId);
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.REJECTED));
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
