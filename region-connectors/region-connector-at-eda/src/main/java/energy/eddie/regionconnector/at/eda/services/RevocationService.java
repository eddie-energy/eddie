package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestProjection;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.dto.EdaCMRevoke;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RevocationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RevocationService.class);
    private final AtPermissionRequestRepository repository;
    private final Outbox outbox;

    public RevocationService(EdaAdapter edaAdapter, AtPermissionRequestRepository repository, Outbox outbox) {
        this.repository = repository;
        this.outbox = outbox;
        edaAdapter.getCMRevokeStream()
                  .subscribe(this::onRevocation);
    }

    private void onRevocation(EdaCMRevoke cmRevoke) {
        String consentId = cmRevoke.consentId();
        var optionalRequest = repository.findByConsentId(consentId);
        if (optionalRequest.isPresent()) {
            var projection = EdaPermissionRequest.fromProjection(optionalRequest.get());
            revoke(projection);
            return;
        }
        LOGGER.atWarn()
              .addArgument(consentId)
              .log("Got CM Revoke Message with unknown consent id: {}; Using fallback and querying permission request by metering point id and revoke date.");

        var requests = fallback(cmRevoke);
        if (requests.isEmpty()) {
            LOGGER.error("Got Revoke Message with unknown consent id: {}; Could not revoke.", consentId);
        } else {
            // Revoke every permission since we do not know which permission request was actually revoked
            requests.forEach(request ->
                                     revoke(EdaPermissionRequest.fromProjection(request)));
        }
    }

    private void revoke(AtPermissionRequest permissionRequest) {
        if (permissionRequest.status() != PermissionProcessStatus.ACCEPTED) {
            return;
        }
        outbox.commit(new SimpleEvent(permissionRequest.permissionId(), PermissionProcessStatus.REVOKED));
        LOGGER.atInfo()
              .addArgument(permissionRequest::permissionId)
              .log("Revoking permission for permission id {}");
    }

    private List<AtPermissionRequestProjection> fallback(EdaCMRevoke cmRevoke) {
        return repository.findAcceptedAndFulfilledByMeteringPointIdAndDate(cmRevoke.meteringPoint(),
                                                                           cmRevoke.consentEnd());
    }
}
