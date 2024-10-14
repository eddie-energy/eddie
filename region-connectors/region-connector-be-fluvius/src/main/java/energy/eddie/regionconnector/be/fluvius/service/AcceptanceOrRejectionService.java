package energy.eddie.regionconnector.be.fluvius.service;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.be.fluvius.client.model.GetMandateResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.clients.FluviusApi;
import energy.eddie.regionconnector.be.fluvius.permission.events.AcceptedEvent;
import energy.eddie.regionconnector.be.fluvius.permission.events.SimpleEvent;
import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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
            fluviusApi.mandateFor(permissionId)
                      .subscribe(
                              res -> handleSuccess(res, permissionRequest),
                              error -> LOGGER.warn(
                                      "Unexpected error when requesting status of permission request {}",
                                      permissionId,
                                      error
                              )
                      );
        }
    }

    private void handleSuccess(GetMandateResponseModelApiDataResponse res, FluviusPermissionRequest permissionRequest) {
        var permissionId = permissionRequest.permissionId();
        if (res.getData() == null || res.getData().getMandates() == null || res.getData().getMandates().isEmpty()) {
            LOGGER.info("No mandates found for permission request {}", permissionId);
            return;
        }
        var status = res.getData().getMandates().getFirst().getStatus();
        switch (status) {
            case "Requested" -> LOGGER.info("Status of Permission request {} has not changed", permissionId);
            case "Approved" -> {
                LOGGER.info("Permission request approved {}", permissionId);
                outbox.commit(new AcceptedEvent(permissionId, res.getData().getMandates().getFirst().getEanNumber()));
            }
            case "Rejected" -> {
                LOGGER.info("Permission request rejected {}", permissionId);
                outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.REJECTED));
            }
            case null, default -> LOGGER.warn("Unexpected permission request {} has status {}", permissionId, status);
        }
    }
}
